package br.com.lojasrenner.rlog.transport.order.business.domain.query;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.model.Quotes;
import br.com.lojasrenner.rlog.transport.order.business.model.QuotationDTO;
import br.com.lojasrenner.rlog.transport.order.business.util.QueryUtil;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.CountryEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.FulfillmentMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ShippingMethodEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.TimeUnityEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.FreightServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BusinessDaysModel;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteBusinessDaysResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteDeliveryOptionsResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.QuoteResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOption;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class PrepareDeliveryOptionsResponseBusiness {

	@Autowired
	private LiveConfig config;

	@Autowired
	private FreightServiceV1 freightService;

	@Autowired
	private BranchOfficeCachedServiceV1 branchOfficeService;

	@Autowired
	private EcommBusiness ecommBusiness;

	public DeliveryOptionsReturn buildResponse(
			DeliveryOptionsRequest deliveryOptionsRequest,
			Quotes quotes
	) {
		List<DeliveryOption> deliveryOptions = new ArrayList<>();

		QuotationDTO quoteFromEcomm = quotes.getQuoteFromEcomm();
		QuotationDTO quoteFromPreSale = quotes.getQuoteFromPreSale();

		final QuotationDTO quote = choseFromStoreOrEcomm(quotes.getQuoteFromStore(), quoteFromEcomm);

		if (quote.getItemListMap() != null) {
			deliveryOptionsRequest.setFinalItemBranchMap(quote.getItemListMap());
			deliveryOptionsRequest.setFinalQuoteMap(quote.getQuoteMap());
			deliveryOptionsRequest.setFinalPickupOptionsReturnMap(quote.getPickupOptionsReturnMap());

			List<CartItem> excessItems = deliveryOptionsRequest.getExcessItems();
			if (excessItems != null && !excessItems.isEmpty()) {
				quote.getItemListMap().computeIfAbsent("0", k -> new ArrayList<>());
				quote.getItemListMap().get("0").addAll(excessItems);
			}

			boolean shouldDisplayScheduled = quoteFromEcomm.getItemListMap() != null &&
					quote.getItemListMap().entrySet().stream().allMatch(entry -> {
						if (entry.getKey().equals("0"))
							return true;

						BranchOfficeEntity ecomm = ecommBusiness.getEcommBranchOffice(deliveryOptionsRequest.getCompanyId(), deliveryOptionsRequest.getXApplicationName());
						return quoteFromEcomm.getItemListMap() != null && quoteFromEcomm.getItemListMap().containsKey(ecomm.getBranchOfficeId()) && entry.getValue()
								.stream()
								.allMatch(i1 -> quoteFromEcomm.getItemListMap()
										.get(ecomm.getBranchOfficeId())
										.stream()
										.anyMatch(i2 -> i1.getSku().equals(i2.getSku()))
								);
					});

			quote.getItemListMap().forEach((originBranch, items) -> {
				Optional<QuoteResponseV1> quoteResponseV1Optional = Optional.ofNullable(quote.getQuoteMap())
						.map(quoteMap -> quoteMap.entrySet()
								.stream()
								.filter(entry -> !entry.getKey().equals("0"))
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
								.get(originBranch));

				quoteResponseV1Optional.ifPresentOrElse(quoteResponseV1 -> {
					final Optional<PickupOptionsReturn> pickupOptions = Optional.ofNullable(quote.getPickupOptionsReturnMap())
							.map(pickupOptionsReturnMap -> pickupOptionsReturnMap.get(originBranch));

					deliveryOptions.addAll(items.stream()
							.map(item -> mapToDeliveryOptionRegular(item, deliveryOptionsRequest, originBranch, quoteResponseV1, pickupOptions, quoteFromEcomm, shouldDisplayScheduled))
							.collect(Collectors.toList()));
				}, () -> deliveryOptions.addAll(items.stream()
						.map(item -> mapToDeliveryOptionRegular(item, deliveryOptionsRequest, originBranch, null, Optional.empty(), quoteFromEcomm, false))
						.collect(Collectors.toList())));
			});

		}

		if (quoteFromPreSale != null && quoteFromPreSale.getItemListMap() != null) {
			AtomicInteger count = new AtomicInteger(1);
			quoteFromPreSale.getItemListMap().forEach((key, items) -> {
				String originBranch = ecommBusiness.getEcommBranchOffice(deliveryOptionsRequest.getCompanyId(), deliveryOptionsRequest.getXApplicationName()).getBranchOfficeId();
				Optional<QuoteResponseV1> preSaleQuote = Optional.ofNullable(quoteFromPreSale.getQuoteMap())
						.map(quotePreSaleMap -> quotePreSaleMap.get(key));

				preSaleQuote.ifPresentOrElse(quoteResponseV1 -> {
							final Optional<PickupOptionsReturn> pickupOptions = Optional.ofNullable(quoteFromPreSale.getPickupOptionsReturnMap())
									.map(quotePreSaleMap -> quotePreSaleMap.get(key));

							deliveryOptions.addAll(items.stream()
									.map(item -> {
										DeliveryOption deliveryOption = mapToDeliveryOptionRegular(item, deliveryOptionsRequest, originBranch, quoteResponseV1, pickupOptions, quoteFromEcomm, false);
										deliveryOption.getDeliveryModes().forEach(mode -> {
											mode.setModalId(mode.getModalId() + '-' + count.get());
											mode.setId(mode.getId() + '-' + count.get());
										});
										return deliveryOption;
									})
									.collect(Collectors.toList()));
						}, () -> deliveryOptions.addAll(items.stream()
								.map(item -> mapToDeliveryOptionRegular(item, deliveryOptionsRequest, originBranch, null, Optional.empty(), quoteFromEcomm, false))
								.collect(Collectors.toList()))
				);

				count.getAndIncrement();
			});
		}

		List<OriginPreview> originMapPreview = buildOriginPreview(deliveryOptionsRequest);

		return DeliveryOptionsReturn.builder()
				.id(deliveryOptionsRequest.getId())
				.date(LocalDateTime.now())
				.deliveryOptions(deliveryOptions)
				.distinctOrigins(quote.getQuoteMap() != null ? quote.getQuoteMap().size() : 0)
				.originPreview(originMapPreview)
				.build();
	}

	private List<OriginPreview> buildOriginPreview(DeliveryOptionsRequest deliveryOptionsRequest) {
		List<OriginPreview> originMapPreview;
		Optional<Map<String, List<CartItem>>> finalItemBranchMapOptional = Optional.ofNullable(deliveryOptionsRequest.getFinalItemBranchMap());
		originMapPreview = finalItemBranchMapOptional.map(itemBranchMap -> itemBranchMap.entrySet()
				.stream()
				.map(entry -> OriginPreview
						.builder()
						.branchId(entry.getKey())
						.skus(entry.getValue()
								.stream()
								.map(CartItem::getSku)
								.collect(Collectors.toList()))
						.build())
				.collect(Collectors.toList())
		).orElse(new ArrayList<>());

		Optional<Map<String, List<CartItem>>> preSaleItemBranchMapOptional = Optional.ofNullable(deliveryOptionsRequest.getPreSaleItemBranchMap());
		originMapPreview.addAll(preSaleItemBranchMapOptional.map(preSaleItemBranchMap -> {
			AtomicInteger count = new AtomicInteger(1);
			BranchOfficeEntity ecom = ecommBusiness.getEcommBranchOffice(deliveryOptionsRequest.getCompanyId(), deliveryOptionsRequest.getXApplicationName());
			return preSaleItemBranchMap.entrySet().stream().map(entry -> {
				OriginPreview origin = OriginPreview.builder()
						.branchId(ecom.getBranchOfficeId() + "-" + count)
						.skus(entry.getValue()
								.stream()
								.map(CartItem::getSku)
								.collect(Collectors.toList()))
						.build();
				count.getAndIncrement();
				return origin;
			}).collect(Collectors.toList());
		}).orElse(new ArrayList<>()));
		return originMapPreview;
	}

	private QuotationDTO choseFromStoreOrEcomm(QuotationDTO quoteFromStore, QuotationDTO quoteFromEcomm) {
		if (quoteFromStore.getItemListMap() != null && !quoteFromStore.getItemListMap().isEmpty()) {
			if (quoteFromEcomm.getItemListMap() != null &&
					productsAvailableCount(quoteFromStore.getItemListMap()) < productsAvailableCount(quoteFromEcomm.getItemListMap()))
				return quoteFromEcomm;

			return quoteFromStore;
		}

		return quoteFromEcomm;
	}

	public int productsAvailableCount(Map<String, List<CartItem>> itemListMap) {
		return itemListMap.entrySet()
				.stream()
				.filter(entry -> !entry.getKey().equals("0"))
				.map(entry -> entry.getValue().size())
				.collect(Collectors.summingInt(s -> s));
	}

	private DeliveryOption mapToDeliveryOptionRegular(CartItem item,
													  DeliveryOptionsRequest deliveryOptionsRequest,
													  String originBranch,
													  QuoteResponseV1 quoteResponseV1,
													  Optional<PickupOptionsReturn> pickupOptions,
													  QuotationDTO quoteFromEcomm,
													  boolean shouldDisplayScheduled) {
		String fulfillmentMethod = null;
		if (!originBranch.equals("0"))
			fulfillmentMethod = branchOfficeService.getBranchOffice(deliveryOptionsRequest.getCompanyId(), originBranch).getConfiguration().getCdManagement();

		String freightCostCurrency = config.getConfigValueString(deliveryOptionsRequest.getCompanyId(),
				Optional.ofNullable(deliveryOptionsRequest.getXApplicationName()),
				CompanyConfigEntity::getFreightCostCurrency,
				true);

		List<DeliveryMode> deliveryModes = new ArrayList<>();
		if (quoteResponseV1 != null) {
			BranchOfficeEntity branchOfficeEntity = branchOfficeService.getBranchOffice(deliveryOptionsRequest.getCompanyId(), originBranch);
			deliveryModes.addAll(mapDeliveryModes(quoteResponseV1, branchOfficeEntity, fulfillmentMethod, freightCostCurrency));
		}

		if (pickupOptions.isPresent())
			deliveryModes.addAll(mapDeliveryModesForPickup(pickupOptions.get(),
					deliveryOptionsRequest.getCompanyId(),
					deliveryOptionsRequest.getXApplicationName(),
					freightCostCurrency));

		if (shouldDisplayScheduled)
			deliveryModes.addAll(mapDeliveryModesForScheduling(quoteFromEcomm, freightCostCurrency, deliveryOptionsRequest.getCompanyId()));

		setBestOptions(deliveryModes);

		fillDeliveryDate(deliveryOptionsRequest, deliveryModes);

		List<DeliveryMode> deliveryModesVerbose = new ArrayList<>(deliveryModes);

		//remove deliveryModes que nao tem campo de data
		deliveryModes = deliveryModes.stream()
				.filter(d -> d.getEstimatedDeliveryDate() != null)
				.collect(Collectors.toList());

		// remove o que nao é recomendação caso não seja request verboso
		if (!deliveryOptionsRequest.isVerbose())
			deliveryModes.removeAll(deliveryModes.stream().filter(d -> !d.getIsRecommendation()).collect(Collectors.toList()));

		//zera logistic info se nao for necessário retornar
		if (!deliveryOptionsRequest.isLogisticInfo())
			deliveryModes.forEach(d -> d.setLogisticCarrierInfo(null));

		deliveryModes.sort(Comparator.comparing(m -> m.getShippingMethod().getOrder()));

		return DeliveryOption.builder()
				.quantity(item.getQuantity())
				.sku(item.getSku())
				.deliveryModes(deliveryModes)
				.deliveryModesVerbose(deliveryModesVerbose)
				.build();
	}

	private static final List<ShippingMethodEnum> TYPES_FOR_SCHEDULED_QUOTATION = Arrays.asList(ShippingMethodEnum.SCHEDULED);

	private Collection<? extends DeliveryMode> mapDeliveryModesForScheduling(QuotationDTO quoteFromEcomm, String freightCostCurrency, String companyId) {
		List<DeliveryMode> list = new ArrayList<>();

		if (quoteFromEcomm == null || quoteFromEcomm.getQuoteMap() == null || quoteFromEcomm.getQuoteMap().isEmpty())
			return list;

		QuoteResponseV1 quoteResponseV1 = quoteFromEcomm.getQuoteMap().values().iterator().next();
		String branchId = quoteFromEcomm.getQuoteMap().keySet().iterator().next();

		Optional<BranchOfficeEntity> ecomm = branchOfficeService.getEcommBranchOffices(companyId).stream()
				.filter(b -> b.getBranchOfficeId().equals(branchId))
				.findFirst();

		String fulfillmentMethod = ecomm.map(b -> b.getConfiguration().getCdManagement()).orElse(FulfillmentMethodEnum.CD.getValue());

		if(ecomm.isEmpty())
			return list;

		List<QuoteDeliveryOptionsResponseV1> deliveryOptions = quoteResponseV1.getContent().getDeliveryOptions();

		List<QuoteDeliveryOptionsResponseV1> deliveryOptionsNotRemovedByQuoteRules = deliveryOptions.stream()
				.filter(d -> !d.isRemovedByQuoteRules())
				.collect(Collectors.toList());

		deliveryOptionsNotRemovedByQuoteRules.forEach(intelipostDeliveryOption -> {
			if (TYPES_FOR_SCHEDULED_QUOTATION.contains(ShippingMethodEnum.fromValue(intelipostDeliveryOption.getDeliveryMethodType())))
				list.add(mapDeliveryMode(quoteResponseV1, ecomm.get(), intelipostDeliveryOption, fulfillmentMethod, freightCostCurrency));
		});

		return list;
	}

	private List<DeliveryMode> mapDeliveryModesForPickup(PickupOptionsReturn pickupOptions, String companyId, String channel, String freightCostCurrency) {
		List<DeliveryMode> list = new ArrayList<>();

		if (pickupOptions == null || pickupOptions.getPickupOptions().isEmpty())
			return list;

		pickupOptions.getPickupOptions().sort((a, b) -> a.getDeliveryEstimateBusinessDays() - b.getDeliveryEstimateBusinessDays());

		for (PickupOption option : pickupOptions.getPickupOptions()) {
			DeliveryMode deliveryMode = mapDeliveryModeForPickup(option, companyId, channel, freightCostCurrency);
			if (deliveryMode != null) {
				list.add(deliveryMode);
				break;
			}
		}

		return list;
	}

	private void fillDeliveryDate(DeliveryOptionsRequest deliveryOptionsRequest, List<DeliveryMode> deliveryModes) {
		Set<BusinessDaysModel> businessDaysList = getBusinessDaysList(deliveryOptionsRequest.getCompanyId(), deliveryModes.stream()
				.filter(d -> d.getIsRecommendation() || deliveryOptionsRequest.isVerbose())
				.filter(d -> d.getEstimatedDeliveryDate() == null)
				.collect(Collectors.toList()));
		Map<String, QuoteBusinessDaysResponseV1> multipleDeliveryDates = freightService.getMultipleDeliveryDates(businessDaysList);

		deliveryModes.forEach(m -> {
			String key = BusinessDaysModel.builder()
					.companyId(deliveryOptionsRequest.getCompanyId())
					.originZipCode(m.getOrigin()).destinationZipCode(m.getDestination())
					.businessDays(m.getDeliveryEstimateBusinessDays())
					.build()
					.getKey();

			QuoteBusinessDaysResponseV1 businessDaysResponse = multipleDeliveryDates.get(key);

			if (businessDaysResponse == null)
				return;

			if (m.getEstimatedDeliveryDate() == null)
				m.setEstimatedDeliveryDate(businessDaysResponse.getDateDelivery());

		});
	}

	private static final List<ShippingMethodEnum> TYPES_FOR_STORE_QUOTATION = Arrays.asList(ShippingMethodEnum.EXPRESS, ShippingMethodEnum.STANDARD);

	private static List<DeliveryMode> mapDeliveryModes(QuoteResponseV1 deliveryModesFromStore, BranchOfficeEntity originBranch, String fulfillmentMethod, String freightCostCurrency) {
		List<DeliveryMode> deliveryModes = new ArrayList<>();

		if (deliveryModesFromStore != null) {
			List<QuoteDeliveryOptionsResponseV1> deliveryOptions = deliveryModesFromStore.getContent().getDeliveryOptions();

			List<QuoteDeliveryOptionsResponseV1> deliveryOptionsNotRemovedByQuoteRules = deliveryOptions.stream()
					.filter(d -> !d.isRemovedByQuoteRules())
					.collect(Collectors.toList());

			deliveryOptionsNotRemovedByQuoteRules.forEach(intelipostDeliveryOption -> {
				if (TYPES_FOR_STORE_QUOTATION.contains(ShippingMethodEnum.fromValue(intelipostDeliveryOption.getDeliveryMethodType())))
					deliveryModes.add(mapDeliveryMode(deliveryModesFromStore, originBranch, intelipostDeliveryOption, fulfillmentMethod, freightCostCurrency));
			});
		}

		return deliveryModes;
	}

	private static Set<BusinessDaysModel> getBusinessDaysList(String companyId, List<DeliveryMode> deliveryModes) {
		Set<BusinessDaysModel> businessDaysToSearch = new HashSet<>();

		deliveryModes.forEach(m -> businessDaysToSearch.add(BusinessDaysModel.builder()
				.companyId(companyId)
				.originZipCode(m.getOrigin().replace("-", ""))
				.destinationZipCode(m.getDestination().replace("-", ""))
				.businessDays(m.getDeliveryEstimateBusinessDays())
				.build()));

		return businessDaysToSearch;
	}

	private DeliveryMode mapDeliveryModeForPickup(PickupOption pickupOption, String companyId, String channel, String freightCostCurrency) {
		Optional<BranchOfficeEntity> branchOptional = branchOfficeService.getBranchOffices(companyId)
				.stream()
				.filter(b -> b.getBranchOfficeId().equals(pickupOption.getBranchId()))
				.findFirst();

		if (branchOptional.isEmpty())
			return null;

		BranchOfficeEntity branchOffice = branchOptional.get();

		Optional<CountryEnum> countryEnum = Optional.ofNullable(CountryEnum.fromName(config.getConfigValueString(companyId, Optional.ofNullable(channel), CompanyConfigEntity::getCountry, false)));

		return DeliveryMode.builder()
				.description("Retire na loja")
				.displayName("Retire na loja")
				.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
				.estimatedDeliveryTimeValue(pickupOption.getDeliveryTime())
				.deliveryEstimateBusinessDays(pickupOption.getDeliveryEstimateBusinessDays())
				.freightCost(0.0)
				.freightCostCurrency(freightCostCurrency)
				.fulfillmentMethod(pickupOption.getFulfillmentMethod())
				.origin(branchOffice.getQuoteZipCode(countryEnum).replace("-", ""))
				.destination(branchOffice.getQuoteZipCode(countryEnum).replace("-", ""))
				.state(pickupOption.getState())
				.isRecommendation(false)
				.shippingMethod(ShippingMethodEnum.PICKUP)
				.branchOfficeId(branchOffice.getBranchOfficeId())
				.originBranchOfficeId(branchOffice.getBranchOfficeId())
				.logisticCarrierInfo(getLogisticCarrierInfo(pickupOption.getFulfillmentMethod(), pickupOption.getQuoteDeliveryOption(), pickupOption.getQuotationId()))
				.id(DeliveryMode.generateModalId(pickupOption.getFulfillmentMethod(), ShippingMethodEnum.PICKUP, pickupOption.getDeliveryEstimateBusinessDays(), 0.0, pickupOption.getOriginBranchOfficeId()))
				.modalId(DeliveryMode.generateModalId(pickupOption.getFulfillmentMethod(), ShippingMethodEnum.PICKUP, pickupOption.getDeliveryEstimateBusinessDays(), 0.0, pickupOption.getOriginBranchOfficeId()))
				.build();
	}

	private void setBestOptions(List<DeliveryMode> deliveryModes) {
		setBestStandardOption(deliveryModes);
		setBestExpressOption(deliveryModes);
		setBestScheduledOption(deliveryModes);
		setBestPickupOption(deliveryModes);
	}

	private void setBestPickupOption(List<DeliveryMode> deliveryModes) {
		List<DeliveryMode> sortedPickups = deliveryModes.stream()
				.filter(d -> d.getShippingMethod() == ShippingMethodEnum.PICKUP)
				.sorted(QueryUtil::emptyBranchOfficeId)
				.sorted(QueryUtil::fulfillmentTypeCDFirst)
				.sorted(QueryUtil::lowestDeliveryEstimate)
				.collect(Collectors.toList());

		//o que está em primeiro é o mais proximo dentre os que tem stock
		if (!sortedPickups.isEmpty())
			sortedPickups.get(0).setIsRecommendation(true);
	}

	private void setBestScheduledOption(List<DeliveryMode> deliveryModes) {
		List<DeliveryMode> scheduledPickups = deliveryModes.stream()
				.filter(d -> d.getShippingMethod() == ShippingMethodEnum.SCHEDULED)
				.sorted(QueryUtil::fulfillmentTypeStoreFirst)
				.sorted(QueryUtil::lowestFreightCost)
				.collect(Collectors.toList());

		if (!scheduledPickups.isEmpty())
			scheduledPickups.get(0).setIsRecommendation(true);
	}

	private void setBestExpressOption(List<DeliveryMode> deliveryModes) {
		Optional<DeliveryMode> fastest = deliveryModes.stream()
				.filter(d -> d.getShippingMethod() == ShippingMethodEnum.EXPRESS)
				.sorted(QueryUtil::fulfillmentTypeStoreFirst)
				.sorted(QueryUtil::lowestFreightCost)
				.min(QueryUtil::lowestDeliveryEstimate);

		fastest.ifPresent(deliveryMode -> deliveryMode.setIsRecommendation(true));
	}

	private void setBestStandardOption(List<DeliveryMode> deliveryModes) {
		Optional<DeliveryMode> fastest = deliveryModes.stream()
				.filter(d -> d.getShippingMethod() == ShippingMethodEnum.STANDARD)
				.sorted(QueryUtil::fulfillmentTypeStoreFirst)
				.sorted(QueryUtil::lowestDeliveryEstimate)
				.min(QueryUtil::lowestFreightCost);

		fastest.ifPresent(deliveryMode -> deliveryMode.setIsRecommendation(true));
	}


	private static DeliveryMode mapDeliveryMode(QuoteResponseV1 deliveryModes,
												BranchOfficeEntity branchOffice,
												QuoteDeliveryOptionsResponseV1 intelipostDeliveryOption,
												String fulfillmentMethod,
												String freightCostCurrency) {

		Long quotationId = deliveryModes.getContent().getId();
		Double providerShippingCost = intelipostDeliveryOption.getProviderShippingCost();
		Double finalShippingCost = intelipostDeliveryOption.getFinalShippingCost();
		ShippingMethodEnum shippingMethod = ShippingMethodEnum.fromValue(intelipostDeliveryOption.getDeliveryMethodType());
		String branchOfficeId = branchOffice.getBranchOfficeId();

		if (shippingMethod == ShippingMethodEnum.PICKUP)
			finalShippingCost = 0.0;

		return DeliveryMode.builder()
				.description(intelipostDeliveryOption.getDescription())
				.displayName(intelipostDeliveryOption.getDeliveryMethodName())
				.deliveryEstimateBusinessDays(intelipostDeliveryOption.getDeliveryEstimateBusinessDays())
				.estimatedDeliveryTimeUnit(TimeUnityEnum.DAY)
				.estimatedDeliveryTimeValue(intelipostDeliveryOption.getDeliveryEstimateBusinessDays().toString())
				.freightCost(finalShippingCost)
				.providerShippingCost(providerShippingCost)
				.freightCostCurrency(freightCostCurrency)
				.fulfillmentMethod(fulfillmentMethod)
				.isRecommendation(false)
				.shippingMethod(shippingMethod)
				.origin(deliveryModes
						.getContent()
						.getOriginZipCode()
						.replace("-", ""))
				.destination(deliveryModes
						.getContent()
						.getDestinationZipCode()
						.replace("-", ""))
				.deliveryMethodId(intelipostDeliveryOption.getDeliveryMethodId())
				.quotationId(quotationId)
				.state(branchOffice.getState())
				.originBranchOfficeId(branchOfficeId)
				.logisticCarrierInfo(getLogisticCarrierInfo(fulfillmentMethod, intelipostDeliveryOption, quotationId))
				.id(DeliveryMode.generateModalId(fulfillmentMethod, shippingMethod, intelipostDeliveryOption.getDeliveryEstimateBusinessDays(), finalShippingCost, branchOfficeId))
				.modalId(DeliveryMode.generateModalId(fulfillmentMethod, shippingMethod, intelipostDeliveryOption.getDeliveryEstimateBusinessDays(), finalShippingCost, branchOfficeId))
				.estimatedDeliveryDate(intelipostDeliveryOption.getDeliveryEstimateDateExactIso())
				.build();
	}

	private static LogisticCarrier getLogisticCarrierInfo(String fulfillmentMethod, QuoteDeliveryOptionsResponseV1 intelipostDeliveryOption, Long quotationId) {
		LogisticCarrier carrier = null;

		if (FulfillmentMethodEnum.CD.isMatch(fulfillmentMethod)) {
			carrier = LogisticCarrier.builder()
					.provider(intelipostDeliveryOption.getLogisticProviderName())
					.quotationId(quotationId)
					.deliveryMethodId(intelipostDeliveryOption.getDeliveryMethodId())
					.isPickupEnabled(intelipostDeliveryOption.isPickupEnabled())
					.isSchedulingEnabled(intelipostDeliveryOption.isSchedulingEnabled())
					.description(intelipostDeliveryOption.getDescription())
					.deliveryMethodType(intelipostDeliveryOption.getDeliveryMethodType())
					.build();
		}

		return carrier;
	}
}
