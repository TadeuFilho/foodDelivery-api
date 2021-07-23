package br.com.lojasrenner.rlog.transport.order.business;

import br.com.lojasrenner.rlog.transport.order.business.domain.fulfillment.OriginBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.fulfillment.model.ReQuoteShipping;
import br.com.lojasrenner.rlog.transport.order.business.exception.*;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.PickupOptionsReactiveDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.CheckoutServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.FreightServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.metrics.BadRequestMetrics;
import br.com.lojasrenner.exception.BadRequestException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.FulfillmentController;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.*;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Log4j2  
public class FulfillmentBusiness {

	@Autowired
	private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

	@Autowired
	private PickupOptionsReactiveDBInfrastructure pickupOptionsDB;

	@Autowired
	private QueryBusiness queryBusiness;

	@Autowired
	private PickupBusiness pickupBusiness;

	@Autowired
	private EcommBusiness ecommBusiness;

	@Autowired
	private BadRequestMetrics badRequestMetrics;

	@Autowired
	private StockBusiness stockBusiness;

	@Autowired
	private BranchOfficeCachedServiceV1 branchService;

	@Autowired
	private LiveConfig config;

	@Autowired
	private CheckoutServiceV1 checkoutService;

	private static final String UNAVAILABLE = "UNAVAILABLE";
	private static final String UNALLOCATED_INVENTORY = "0";

	public CartOrderResult getSimplifiedDeliveryFulfillmentForShoppingCart(FulfillmentRequest fulfillmentRequest) throws NoQuotationAvailableForFulfillment {
		final String cartOrderId = fulfillmentRequest.getCartOrder().getId();
		final String companyId = fulfillmentRequest.getCompanyId();
		final String channel = fulfillmentRequest.getXApplicationName();

		fulfillmentRequest.setDeliveryOptionsRequestId(cartOrderId);

		DeliveryOptionsRequest deliveryOptions = findDeliveryOptionsRequest(cartOrderId, companyId, channel, fulfillmentRequest);

		if (!fulfillmentRequest.getAutoReQuoteHasChanged() && shouldNotifyCheckoutService(deliveryOptions))
			checkoutService.checkoutQuotation(companyId, deliveryOptions.getId(), deliveryOptions.getShoppingCart().getExtraIdentification().getExtOrderCode());

		fulfillmentRequest.setDeliveryOptionsRequest(deliveryOptions);

		List<PickupOptionsRequest> pickupOptionsRequestList = findPickupOptionsRequest(fulfillmentRequest);
		fulfillmentRequest.setPickupOptionsRequestList(pickupOptionsRequestList);

		Map<String, List<CartItemWithMode>> newItemMap = new HashMap<>();
		DeliveryOptionsRequest reQuoteDeliveryOptions = null;
		PickupOptionsRequest reQuotePickupOptionsRequest = null;

		if (!fulfillmentRequest.getAutoReQuoteHasChanged()) {
			linkCartItemFromFulfillWithCartItemFromQuery(fulfillmentRequest, deliveryOptions);

			Map<String, List<CartItemWithMode>> itemMap = validateSkusAndModalIds(fulfillmentRequest, deliveryOptions, pickupOptionsRequestList);
			fulfillmentRequest.getItemMapList().add(itemMap);

			if (fulfillmentRequest.getShippingMethod().getShippingType().equals(ShippingTypeEnum.SHIPPING)) {
				final ReQuoteShipping reQuoteShipping = reQuoteShipping(fulfillmentRequest, deliveryOptions).orElseThrow(RuntimeException::new);
				reQuoteDeliveryOptions = reQuoteShipping.getDeliveryOptionsOriginType() == DeliveryOptionsOriginTypeEnum.OK_SAME_ORIGIN ? null : reQuoteShipping.getDeliveryOptionsRequest();
				newItemMap = reQuoteShipping.getItems();
			} else {
				reQuotePickupOptionsRequest = reQuotePickup(fulfillmentRequest, newItemMap);
			}
		} else {
			Map<String, List<CartItemWithMode>> autoItemMap = buildAutoItemMap(fulfillmentRequest, deliveryOptions);
			fulfillmentRequest.getItemMapList().add(autoItemMap);

			autoReQuoteNewItemMap(fulfillmentRequest, deliveryOptions, newItemMap);
		}

		fulfillmentRequest.getItemMapList().add(newItemMap);

		if (reQuoteDeliveryOptions != null || reQuotePickupOptionsRequest != null)
			newItemMap = linkSkusWithNewDeliveryMode(fulfillmentRequest, reQuoteDeliveryOptions, reQuotePickupOptionsRequest, newItemMap);

		fulfillmentRequest.getItemMapList().add(newItemMap);

		boolean fulfillmentHasChanged = reQuoteDeliveryOptions != null || reQuotePickupOptionsRequest != null;

		return buildSimpleFulfillmentResponse(fulfillmentRequest, newItemMap, fulfillmentHasChanged);
	}

	private boolean shouldNotifyCheckoutService(DeliveryOptionsRequest deliveryOptions) {
		return deliveryOptions.getCheckout() == null || deliveryOptions.getCheckout().getHasLock() == null || !deliveryOptions.getCheckout().getHasLock();
	}

	private Map<String, List<CartItemWithMode>> buildAutoItemMap(FulfillmentRequest fulfillmentRequest, DeliveryOptionsRequest deliveryOptions) {
		Map<String, List<CartItemWithMode>> newMapCartItem = new HashMap<>();

		deliveryOptions.getResponse().getOriginPreview().forEach(originPreview -> {
			if (originPreview.getBranchId().equals("0")) return;
			newMapCartItem.put(originPreview.getBranchId(), fulfillmentRequest.getCartOrder().getItems()
					.stream()
					.filter(itemWithMode -> originPreview.getSkus().contains(itemWithMode.getSku()))
					.collect(Collectors.toList()));
		});

		return newMapCartItem;
	}

	private static void autoReQuoteNewItemMap(FulfillmentRequest fulfillmentRequest, DeliveryOptionsRequest deliveryOptions, Map<String, List<CartItemWithMode>> newItemMap) {
		Optional<OriginPreview> originItem = deliveryOptions.getResponse().getOriginPreview()
				.stream()
				.filter(p -> p.getBranchId().equals(fulfillmentRequest.getMainBranch()))
				.findFirst();

		Optional<OriginPreview> unavailableOriginItem = deliveryOptions.getResponse().getOriginPreview()
				.stream()
				.filter(p -> p.getBranchId().equals(UNALLOCATED_INVENTORY))
				.findFirst();

		Optional<OriginPreview> newOriginItem = deliveryOptions.getResponse().getOriginPreview()
				.stream()
				.filter(p -> !p.getBranchId().equals(fulfillmentRequest.getMainBranch()) && !p.getBranchId().equals(UNALLOCATED_INVENTORY))
				.findFirst();

		if (originItem.isPresent() && originItem.get().getSkus().size() == fulfillmentRequest.getItemsList().size()) {
			newItemMap.put(fulfillmentRequest.getMainBranch(), fulfillmentRequest.getCartOrder().getItems());
			fulfillmentRequest.getStatistics().setOriginType(DeliveryOptionsOriginTypeEnum.OK_SAME_ORIGIN);
		} else if (newOriginItem.isPresent() && newOriginItem.get().getSkus().size() == fulfillmentRequest.getItemsList().size()) {
			newItemMap.put(newOriginItem.get().getBranchId(), fulfillmentRequest.getCartOrder().getItems());
			fulfillmentRequest.getStatistics().setOriginType(DeliveryOptionsOriginTypeEnum.OK_NEW_ORIGIN);
		} else if (unavailableOriginItem.isPresent() && unavailableOriginItem.get().getSkus().size() == fulfillmentRequest.getItemsList().size()) {
			newItemMap.put(UNAVAILABLE, fulfillmentRequest.getCartOrder().getItems());
			fulfillmentRequest.getStatistics().setOriginType(DeliveryOptionsOriginTypeEnum.NO_ORIGIN);
		}
	}

	private PickupOptionsRequest reQuotePickup(FulfillmentRequest fulfillmentRequest, Map<String, List<CartItemWithMode>> newItemMap) throws NoQuotationAvailableForFulfillment {
		PickupOptionsRequest pickupOptionsRequest = createReQuotePickupOptionsRequest(fulfillmentRequest);
		AtomicBoolean mapHasChanged = new AtomicBoolean(false);
		try {
			PickupOptionsReturn pickupOptionsReturn = pickupBusiness.getPickupOptions(pickupOptionsRequest);
			pickupOptionsRequest.setResponse(pickupOptionsReturn);

			PickupOption pickupOption = pickupOptionsReturn.getPickupOptions()
					.stream()
					.filter(o -> o.getBranchId().equals(fulfillmentRequest.getMainBranch()))
					.findFirst()
					.map(item -> {
						mapHasChanged.set(handlePickupOptionMainBranch(fulfillmentRequest, newItemMap, item));
						return item;
					}).orElseGet(() -> {
						mapHasChanged.set(true);
						return handlePickupOptionMainBranchEmpty(fulfillmentRequest, newItemMap).orElse(null);
					});

			if (pickupOption != null && pickupOption.getBranchId().equals(pickupOption.getOriginBranchOfficeId()))
				fulfillmentRequest.getStatistics().setStockType(DeliveryOptionsStockTypeEnum.OWN_STOCK);
			else if (pickupOption != null && FulfillmentMethodEnum.CD.notMatch(pickupOption.getFulfillmentMethod()))
				fulfillmentRequest.getStatistics().setStockType(DeliveryOptionsStockTypeEnum.SHIPPING_TO);
			else
				fulfillmentRequest.getStatistics().setStockType(DeliveryOptionsStockTypeEnum.CD);

		} catch (Exception e) {
			fulfillmentRequest.addException("reQuotePickupOptionsReturn", e);
			throw new NoQuotationAvailableForFulfillment("Error requoting pickup to check stock availability and branch status", e);
		}

		fulfillmentRequest.getStatistics().setReason(pickupOptionsRequest.getStatistics().getReason());
		fulfillmentRequest.setReQuotePickupOptionsRequest(pickupOptionsRequest);
		fulfillmentRequest.setReQuotePickupOptionsMapHasChanged(mapHasChanged.get());

		return mapHasChanged.get() ? pickupOptionsRequest : null;
	}

	private Boolean handlePickupOptionMainBranch(FulfillmentRequest fulfillmentRequest, Map<String, List<CartItemWithMode>> newItemMap, final PickupOption pickupOption) {
		if (pickupOption.getOriginBranchOfficeId().equals(fulfillmentRequest.getPickupOption().getOriginBranchOfficeId())) {
			//original ainda disponível
			newItemMap.put(fulfillmentRequest.getMainBranch(), fulfillmentRequest.getCartOrder().getItems());
			fulfillmentRequest.getStatistics().setOriginType(DeliveryOptionsOriginTypeEnum.OK_SAME_ORIGIN);
		} else {
			if (shouldAllowReOrder(fulfillmentRequest, fulfillmentRequest.getMainBranch())) {
				//mudou a origem
				newItemMap.put(pickupOption.getOriginBranchOfficeId(), fulfillmentRequest.getCartOrder().getItems());
				fulfillmentRequest.getStatistics().setOriginType(DeliveryOptionsOriginTypeEnum.OK_NEW_ORIGIN);
			} else {
				newItemMap.put(UNALLOCATED_INVENTORY, fulfillmentRequest.getCartOrder().getItems());
				fulfillmentRequest.getStatistics().setOriginType(DeliveryOptionsOriginTypeEnum.NO_ORIGIN);
			}
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	private Optional<PickupOption> handlePickupOptionMainBranchEmpty(FulfillmentRequest fulfillmentRequest, Map<String, List<CartItemWithMode>> newItemMap) {
		//muito ruim, essa branch nem ta mais na lista de pickup
		PickupOption pickupOption = null;
		if (fulfillmentRequest.getCartOrder().getItems().size() == 1) {
			newItemMap.put(UNALLOCATED_INVENTORY, fulfillmentRequest.getCartOrder().getItems());
			fulfillmentRequest.getStatistics().setOriginType(DeliveryOptionsOriginTypeEnum.NO_ORIGIN);
		} else {
			BranchOfficeEntity branchForFakeOptionForPartial = checkForPartialPickup(fulfillmentRequest, newItemMap);
			if (branchForFakeOptionForPartial != null) {
				fulfillmentRequest.getStatistics().setOriginType(DeliveryOptionsOriginTypeEnum.PARTIAL_SAME_ORIGIN);
				pickupOption = PickupOption.builder()
						.branchId(fulfillmentRequest.getMainBranch())
						.originBranchOfficeId(fulfillmentRequest.getMainBranch())
						.branchType(BranchTypeEnum.STORE)
						.deliveryEstimateBusinessDays(branchForFakeOptionForPartial.getConfiguration().getStoreWithdrawalTerm())
						.deliveryTimeUnit(TimeUnityEnum.DAY)
						.deliveryTime(branchForFakeOptionForPartial.getConfiguration().getStoreWithdrawalTerm() + "")
						.fulfillmentMethod(branchForFakeOptionForPartial.getConfiguration().getCdManagement())
						.build();
			} else {
				fulfillmentRequest.getStatistics().setOriginType(DeliveryOptionsOriginTypeEnum.NO_ORIGIN);
			}
		}
		return Optional.ofNullable(pickupOption);
	}

	private BranchOfficeEntity checkForPartialPickup(FulfillmentRequest fulfillmentRequest,
	                                                 Map<String, List<CartItemWithMode>> newItemMap) {

		BranchOfficeEntity ecommBranch = ecommBusiness.getEcommBranchOffice(fulfillmentRequest.getCompanyId(), fulfillmentRequest.getXApplicationName());

		Optional<BranchOfficeEntity> branch = branchService.getActiveBranchOfficesForPickup(fulfillmentRequest.getCompanyId())
				.stream()
				.filter(b -> b.getBranchOfficeId().equals(fulfillmentRequest.getMainBranch()))
				.findFirst();

		if (branch.isEmpty()) {
			newItemMap.put(UNALLOCATED_INVENTORY, fulfillmentRequest.getCartOrder().getItems());
			return null;
		}

		ResponseEntity<List<LocationStockV1Response>> storeWithStock = stockBusiness.findStoreWithStock(
				fulfillmentRequest.getCompanyId(),
				fulfillmentRequest.getXApplicationName(),
				fulfillmentRequest.getItemsList(),
				Arrays.asList(branch.get())
		);

		List<LocationStockV1Response> stockBody = stockBusiness.overrideStockQuantities(fulfillmentRequest.getItemsList(),
				storeWithStock,
				Arrays.asList(branch.get()),
				ecommBranch);

		LocationStockV1Response bestLocation = stockBusiness.findBestLocation(fulfillmentRequest.getItemsList(),
				stockBody,
				Arrays.asList(branch.get().getBranchOfficeId()),
				emulateGeoResponse(Set.of(fulfillmentRequest.getMainBranch())),
				fulfillmentRequest);

		if (bestLocation == null || fulfillmentRequest.getQuoteSettings().getBlockedBranches().contains(bestLocation.getBranchOfficeId())) {
			newItemMap.put(UNALLOCATED_INVENTORY, fulfillmentRequest.getCartOrder().getItems());
			return null;
		}

		List<CartItemWithMode> unavailable = fulfillmentRequest.getCartOrder().getItems()
				.stream()
				.filter(i -> i.getCartItem().getQuantity() > stockBusiness.getAmountSaleableForItem(i.getSku(), bestLocation))
				.collect(Collectors.toList());

		List<CartItemWithMode> availableOwnStock = fulfillmentRequest.getCartOrder().getItems()
				.stream()
				.filter(i -> i.getCartItem().getQuantity() <= stockBusiness.getAmountSaleableForItem(i.getSku(), bestLocation))
				.collect(Collectors.toList());

		if (!unavailable.isEmpty())
			newItemMap.put(UNALLOCATED_INVENTORY, unavailable);

		if (!availableOwnStock.isEmpty()) {
			newItemMap.put(fulfillmentRequest.getMainBranch(), availableOwnStock);
			return branch.get();
		}

		return null;
	}

	private List<GeoLocationResponseV1> emulateGeoResponse(Set<String> branchesFromGroup) {
		AtomicInteger counter = new AtomicInteger(0);
		return branchesFromGroup.stream()
				.map(b -> GeoLocationResponseV1.builder()
						.branchOfficeId(b)
						.distance(counter.getAndAdd(1))
						.build())
				.collect(Collectors.toList());
	}

	private boolean shouldAllowFulfillWithMaxCapacityAchieved(FulfillmentRequest fulfillmentRequest) {
		Boolean value = config.getConfigValueBoolean(fulfillmentRequest.getCompanyId(),
				Optional.ofNullable(fulfillmentRequest.getXApplicationName()),
				CompanyConfigEntity::getAllowFulfillWithMaxCapacityAchieved,
				false);
		return value != null && value.booleanValue();
	}

	private PickupOptionsRequest createReQuotePickupOptionsRequest(FulfillmentRequest fulfillmentRequest) {
		List<String> extraStatus = new ArrayList<>();

		if (shouldAllowFulfillWithMaxCapacityAchieved(fulfillmentRequest) && fulfillmentRequest.getHasItemOmniStockOnCart().booleanValue())
			extraStatus.addAll(config.getConfigValueAsListOfString(fulfillmentRequest.getCompanyId(),
					Optional.ofNullable(fulfillmentRequest.getXApplicationName()),
					c -> c.getFulfillment().getValidBranchOfficeStatus(), true));

		PickupOptionsRequest pickupOptionsRequest = new PickupOptionsRequest();
		setDefaultParamsFromPickup(fulfillmentRequest, pickupOptionsRequest);
		pickupOptionsRequest.setDeliveryOptionsId(fulfillmentRequest.getPickupOptionsRequest().getDeliveryOptionsId());
		pickupOptionsRequest.setState(fulfillmentRequest.getPickupOptionsRequest().getState());
		pickupOptionsRequest.setZipcode(fulfillmentRequest.getPickupOptionsRequest().getZipcode());
		pickupOptionsRequest.setSkus(fulfillmentRequest.getPickupOptionsRequest().getSkus());
		pickupOptionsRequest.setQuoteSettings(QuoteSettings.builder()
				.reQuotePickup(Boolean.TRUE)
				.blockedBranches(fulfillmentRequest.getQuoteSettings().getBlockedBranches())
				.eagerBranchesHeader(Collections.singletonList(fulfillmentRequest.getMainBranch()))
				.extraBranchStatus(extraStatus)
				.build());

		return pickupOptionsRequest;
	}

	private Optional<ReQuoteShipping> reQuoteShipping(FulfillmentRequest fulfillmentRequest, final DeliveryOptionsRequest deliveryOptionsRequest) throws NoQuotationAvailableForFulfillment {
		Optional<ReQuoteShipping> response;
		DeliveryOptionsRequest reQuoteDeliveryOptionsRequest = createReQuoteDeliveryOptionsRequest(deliveryOptionsRequest, fulfillmentRequest);
		try {
			//tentar cotar forçando a branch original
			DeliveryOptionsReturn deliveryOptionsReturn = queryBusiness.getDeliveryModesForShoppingCart(reQuoteDeliveryOptionsRequest);
			reQuoteDeliveryOptionsRequest.setResponse(deliveryOptionsReturn);
			ReQuoteShipping reQuoteShipping = reQuoteShipping(fulfillmentRequest, deliveryOptionsReturn, reQuoteDeliveryOptionsRequest);
			response = Optional.of(reQuoteShipping);
			fulfillmentRequest.getStatistics().setOriginType(reQuoteShipping.getDeliveryOptionsOriginType());
		} catch (Exception e) {
			fulfillmentRequest.addException("reQuoteDeliveryOptionsReturn", e);
			throw new NoQuotationAvailableForFulfillment("Error requoting shipping to check stock availability and branch status", e);
		}
		fulfillmentRequest.getStatistics().setReason(reQuoteDeliveryOptionsRequest.getStatistics().getReason());
		fulfillmentRequest.setReQuoteDeliveryOptions(reQuoteDeliveryOptionsRequest);
		fulfillmentRequest.setReQuoteDeliveryOptionsMapHasChanged(fulfillmentRequest.getStatistics().getOriginType() != DeliveryOptionsOriginTypeEnum.OK_SAME_ORIGIN);
		return response;
	}

	private ReQuoteShipping reQuoteShipping(FulfillmentRequest fulfillmentRequest, final DeliveryOptionsReturn reQuoteDeliveryOptionsReturn, final DeliveryOptionsRequest reQuoteDeliveryOptionsRequest) {
			Optional<OriginPreview> reQuoteOriginItem = reQuoteDeliveryOptionsReturn.getOriginPreview()
					.stream()
					.filter(p -> p.getBranchId().equals(fulfillmentRequest.getMainBranch()) || p.getBranchId().indexOf(fulfillmentRequest.getMainBranch() + "-") >= 0)
					.findFirst();

			Optional<OriginPreview> unavailableOriginItem = reQuoteDeliveryOptionsReturn.getOriginPreview()
					.stream()
					.filter(p -> p.getBranchId().equals(UNALLOCATED_INVENTORY))
					.findFirst();

			Optional<OriginPreview> newOriginItem = reQuoteDeliveryOptionsReturn.getOriginPreview()
					.stream()
					.filter(p -> !p.getBranchId().equals(fulfillmentRequest.getMainBranch()) && !p.getBranchId().equals(UNALLOCATED_INVENTORY))
					.findFirst();

			final String newOriginBranchId = newOriginItem.map(OriginPreview::getBranchId).orElse(null);

			final String ecommBranchOfficeId = ecommBusiness.getEcommBranchOffice(fulfillmentRequest.getCompanyId(), fulfillmentRequest.getXApplicationName()).getBranchOfficeId();
			final Boolean reOrderActive = config.getConfigValueBoolean(fulfillmentRequest.getCompanyId(), Optional.ofNullable(fulfillmentRequest.getXApplicationName()), c -> c.getReOrder().getActive(), false);

			final Integer skusSize = fulfillmentRequest.getItemsList().size();
			final Integer reQuoteOriginSkusSize = reQuoteOriginItem.map(item -> item.getSkus().size()).orElse(0);
			final Integer newOriginSkuSize = newOriginItem.map(item -> item.getSkus().size()).orElse(0);
			final Integer unavailableOriginSkusSize = unavailableOriginItem.map(item -> item.getSkus().size()).orElse(0);

			var originStrategy = OriginBusiness
					.getOrigin(newOriginBranchId, ecommBranchOfficeId, reOrderActive, skusSize, reQuoteOriginSkusSize, newOriginSkuSize, unavailableOriginSkusSize);

			log.info("id={} || origin={}", fulfillmentRequest.getId(), originStrategy.getOrigin());
			final Map<String, List<CartItemWithMode>> cartItemWithModeMap = originStrategy.getStrategy()
					.getMapCartItemWithMode(fulfillmentRequest.getCartOrder().getItems(), fulfillmentRequest.getMainBranch(), reQuoteOriginItem, unavailableOriginItem, newOriginItem);

			return ReQuoteShipping.builder()
					.items(cartItemWithModeMap)
					.deliveryOptionsOriginType(originStrategy.getOrigin())
					.deliveryOptionsRequest(reQuoteDeliveryOptionsRequest)
					.build();
	}

	private boolean shouldAllowReOrder(FulfillmentRequest fulfillmentRequest, String reQuoteBranch) {
		Boolean value = config.getConfigValueBoolean(fulfillmentRequest.getCompanyId(), Optional.ofNullable(fulfillmentRequest.getXApplicationName()),
				c -> c.getReOrder().getActive(), false);

		BranchOfficeEntity ecommBranchOffice = ecommBusiness.getEcommBranchOffice(fulfillmentRequest.getCompanyId(), fulfillmentRequest.getXApplicationName());

		return (value != null && value.booleanValue()) || ecommBranchOffice.getBranchOfficeId().equals(reQuoteBranch);
	}

	private DeliveryOptionsRequest createReQuoteDeliveryOptionsRequest(DeliveryOptionsRequest deliveryOptions, FulfillmentRequest fulfillmentRequest) {
		DeliveryOptionsRequest brokerRequest = new DeliveryOptionsRequest();

		setDefaultParamsFromQuotation(brokerRequest,
				deliveryOptions.getXApplicationName(),
				deliveryOptions.getXCurrentDate(),
				deliveryOptions.getXLocale(),
				deliveryOptions.getCompanyId());

		brokerRequest.setVerbose(deliveryOptions.isVerbose());
		brokerRequest.setLogisticInfo(deliveryOptions.isLogisticInfo());
		brokerRequest.setShoppingCart(ShoppingCart.builder()
				.containsRestrictedOriginItems(deliveryOptions.getShoppingCart().isContainsRestrictedOriginItems())
				.destination(CartDestination.builder().zipcode(deliveryOptions.getShoppingCart().getDestination().getZipcode()).build())
				.extraIdentification(deliveryOptions.getShoppingCart().getExtraIdentification())
				.items(fulfillmentRequest.getItemsList())
				.build());
		brokerRequest.setQuoteSettings(deliveryOptions.getQuoteSettings());

		//força settings para cenário de fulfill
		brokerRequest.getQuoteSettings().setMaxOriginsStoreHeader(1);
		brokerRequest.getQuoteSettings().setMaxOriginsHeader(1);
		brokerRequest.getQuoteSettings().setEagerBranchesHeader(Collections.singletonList(fulfillmentRequest.getMainBranch()));

		if (shouldAllowFulfillWithMaxCapacityAchieved(fulfillmentRequest) && fulfillmentRequest.getHasItemOmniStockOnCart().booleanValue())
			brokerRequest.getQuoteSettings().setExtraBranchStatus(config.getConfigValueAsListOfString(fulfillmentRequest.getCompanyId(),
					Optional.ofNullable(fulfillmentRequest.getXApplicationName()),
					c -> c.getFulfillment().getValidBranchOfficeStatus(), true));

		brokerRequest.getQuoteSettings().setBlockedBranches(fulfillmentRequest.getQuoteSettings().getBlockedBranches());

		return brokerRequest;
	}

	private static void setDefaultParamsFromQuotation(DeliveryOptionsRequest deliveryOptions, String xApplicationName, String xCurrentDate, String xLocale, String companyId) {
		deliveryOptions.setId(UUID.randomUUID().toString());
		deliveryOptions.setInitialTimestamp(System.currentTimeMillis());
		deliveryOptions.setDate(LocalDateTime.now());
		deliveryOptions.setXApplicationName(xApplicationName);
		deliveryOptions.setXCurrentDate(xCurrentDate);
		deliveryOptions.setXLocale(xLocale);
		deliveryOptions.setCompanyId(companyId);
	}

	private void linkCartItemFromFulfillWithCartItemFromQuery(FulfillmentRequest fulfillmentRequest, final DeliveryOptionsRequest deliveryOptions) {

		fulfillmentRequest.getCartOrder()
				.getItems()
				.forEach(c -> {
					Optional<CartItem> cartItem = deliveryOptions
							.getShoppingCart()
							.getItems()
							.stream()
							.filter(i -> i.getSku().equals(c.getSku()))
							.findFirst();

					cartItem.ifPresent(c::setCartItem);
				});
		if (fulfillmentRequest.getCartOrder().getItems().stream().anyMatch(c -> c.getCartItem() == null)) {
			List<String> skuNotFound = fulfillmentRequest.getCartOrder()
					.getItems()
					.stream()
					.filter(c -> c.getCartItem() == null)
					.map(CartItemWithMode::getSku)
					.collect(Collectors.toList());

			fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);

			badRequestMetrics.sendBadRequestMetrics(fulfillmentRequest.getCompanyId(),
					fulfillmentRequest.getXApplicationName(),
					ReasonTypeEnum.INVALID_SKU,
					FulfillmentBusiness.class.getSimpleName());

			throw new SkuNotFoundException("DeliveryOption requested and not found for skus " + String.join(", ", skuNotFound));
		}
	}

	private CartOrderResult buildSimpleFulfillmentResponse(FulfillmentRequest fulfillmentRequest,
	                                                       Map<String, List<CartItemWithMode>> itemMap,
	                                                       boolean hasChanged) {
		final List<DeliveryGroupFulfillment> unavailableDeliveryGroupList = itemMap.entrySet().parallelStream()
				.filter(entryItemMap -> entryItemMap.getKey().contains(UNAVAILABLE))
				.map(entryItemMap ->
						DeliveryGroupFulfillment.builder()
							.items(buildItemFulfillmentList(entryItemMap.getValue()))
							.unavailable(Boolean.TRUE)
							.build())
				.collect(Collectors.toList());


		final List<DeliveryGroupFulfillment> deliveryGroupListWithoutUnavailable = itemMap.entrySet().parallelStream()
				.filter(entryItemMap -> !entryItemMap.getKey().contains(UNAVAILABLE))
				.map(entryItemMap -> {
					final CartItemWithMode cartItem = entryItemMap.getValue().stream()
							.findAny()
							.orElseThrow(RuntimeException::new);

					return DeliveryGroupFulfillment.builder()
							.items(buildItemFulfillmentList(entryItemMap.getValue()))
							.originBranchId(cartItem.getDeliveryMode().getOriginBranchOfficeId())
							.extQuotationId(Objects.toString(cartItem.getDeliveryMode().getQuotationId(), null))
							.estimatedDeliveryTimeValue(cartItem.getDeliveryMode().getEstimatedDeliveryTimeValue())
							.estimatedDeliveryTimeUnit(cartItem.getDeliveryMode().getEstimatedDeliveryTimeUnit().toString())
							.estimatedDeliveryDate(cartItem.getDeliveryMode().getEstimatedDeliveryDate())
							.extDeliveryMethodId(Objects.toString(cartItem.getDeliveryMode().getDeliveryMethodId(), null))
							.extDeliveryMethodType(cartItem.getDeliveryMode().getShippingMethod())
							.fulfillmentMethod(cartItem.getDeliveryMode().getFulfillmentMethod())
							.freightCostCurrency(cartItem.getDeliveryMode().getFreightCostCurrency())
							.freightCost(buildFreightCost(cartItem.getDeliveryMode(), hasChanged))
							.stockType(buildStockType(fulfillmentRequest))
							.extDescription(cartItem.getDeliveryMode().getDescription())
							.extProvider(
									Optional.ofNullable(cartItem.getDeliveryMode().getLogisticCarrierInfo())
										.map(LogisticCarrier::getProvider)
										.orElse(null))
							.build();
				}).collect(Collectors.toList());

		final List<DeliveryGroupFulfillment> deliveryGroupList = Stream.concat(deliveryGroupListWithoutUnavailable.stream(), unavailableDeliveryGroupList.stream())
				.collect(Collectors.toList());

		return CartOrderResult.builder()
				.id(fulfillmentRequest.getId())
				.originType(fulfillmentRequest.getStatistics().getOriginType())
				.fulfillmentInfo(DeliveryGroup.builder()
						.groups(deliveryGroupList)
						.build())
				.fulfillmentConditionsHasChanged(hasChanged)
				.build();
	}

	public DeliveryOptionsStockTypeEnum buildStockType(final FulfillmentRequest fulfillmentRequest) {
		return fulfillmentRequest.getAutoReQuoteHasChanged() && fulfillmentRequest.getPickupOption() != null
				? fulfillmentRequest.getPickupOption().getStockType()
				: Optional.ofNullable(fulfillmentRequest.getReQuotePickupOptionsRequest())
					.map(PickupOptionsRequest::getResponse)
					.map(PickupOptionsReturn::getPickupOptions)
					.map(item -> item.stream()
							.filter(p -> p.getBranchId().equals(fulfillmentRequest.getMainBranch()))
							.map(PickupOption::getStockType)
							.filter(Objects::nonNull)
							.findFirst()
							.orElse(null)
					).orElse(null);
	}

	private Double buildFreightCost(final DeliveryMode deliveryMode, final boolean hasChanged) {
		if (!hasChanged && deliveryMode.getShippingMethod().equals(ShippingMethodEnum.PICKUP) &&
				FulfillmentMethodEnum.STORE.isMatch(deliveryMode.getFulfillmentMethod())) {
			return 0.0;
		} else {
			return deliveryMode.getFreightCost();
		}
	}

	private List<ItemFulfillment> buildItemFulfillmentList(final List<CartItemWithMode> cartItemWithModes) {
		return cartItemWithModes.stream()
				.map(cartItem -> ItemFulfillment.builder()
						.sku(cartItem.getSku())
						.quantity(cartItem.getCartItem() != null ? cartItem.getCartItem().getQuantity() : cartItem.getQuantity())
						.isOmniStock(StockStatusEnum.isOmniStock(cartItem.getCartItem() != null ? cartItem.getCartItem().getStockStatus() : cartItem.getStockStatus()))
						.build())
				.collect(Collectors.toList());
	}

	private DeliveryOptionsRequest findDeliveryOptionsRequest(final String cartOrderId, final String companyId, final String channel, final FulfillmentRequest fulfillmentRequest) {
		fulfillmentRequest.setAutoReQuoteHasChanged(Boolean.FALSE);

		return deliveryOptionsDB
				.findById(cartOrderId)
				.orElseGet(() -> handleDeliveryOptionsRequestNotFound(cartOrderId, companyId, channel, fulfillmentRequest));
	}

	private DeliveryOptionsRequest handleDeliveryOptionsRequestNotFound(final String cartOrderId, final String companyId, final String channel, final FulfillmentRequest fulfillmentRequest) {
		if (config.getConfigValueBoolean(companyId, Optional.ofNullable(channel), c -> c.getFulfillment().getAutoReQuote(), true)) {
			return autoRequoteDeliveryOptionsRequest(fulfillmentRequest);
		} else {
			badRequestMetrics.sendBadRequestMetrics(
					fulfillmentRequest.getCompanyId(),
					channel,
					ReasonTypeEnum.NOT_FOUND_QUOTE_ID,
					FulfillmentBusiness.class.getSimpleName()
			);
			throw new DeliveryOptionsRequestNotFoundException("DeliveryOptionsRequest not found for id " + cartOrderId);
		}
	}

	private DeliveryOptionsRequest autoRequoteDeliveryOptionsRequest(FulfillmentRequest fulfillmentRequest) {
		try {
			validateRequiredFieldsInAutoQuote(fulfillmentRequest.getCartOrder().getItems(), fulfillmentRequest, fulfillmentRequest.getCompanyId());
			DeliveryOptionsRequest deliveryOptions = preparingDataFromQuotation(fulfillmentRequest);
			DeliveryOptionsReturn deliveryOptionsReturn = queryBusiness.getDeliveryModesForShoppingCart(deliveryOptions);
			deliveryOptions.setResponse(deliveryOptionsReturn);

			String modalId = fulfillmentRequest.getCartOrder().getItems()
					.stream()
					.map(CartItemWithMode::getModalId)
					.findFirst()
					.orElseThrow(() -> new ModalIdNotFoundException("DeliveryOption requested and not found for modalId"));

			Optional<DeliveryMode> deliveryModeOptional = deliveryOptionsReturn.getDeliveryOptions().get(0).getDeliveryModesVerbose()
					.stream()
					.filter(deliveryMode -> deliveryMode.getModalId().equals(modalId))
					.findFirst();

			String shippingMethodSplit = modalId.split("-")[1];
			if (deliveryModeOptional.isEmpty()) {
				deliveryModeOptional = deliveryOptionsReturn.getDeliveryOptions().get(0).getDeliveryModes()
						.stream()
						.filter(deliveryMode -> deliveryMode.getShippingMethod().equals(ShippingMethodEnum.valueOf(shippingMethodSplit)))
						.findFirst();
			}

			Optional<DeliveryMode> finalDeliveryModeOptional = deliveryModeOptional;

			fulfillmentRequest.getCartOrder().getItems().forEach(itemWithMode -> {
				itemWithMode.setModalId(finalDeliveryModeOptional.map(DeliveryMode::getModalId).orElse(null));
				itemWithMode.setDeliveryMode(finalDeliveryModeOptional.orElse(null));
			});
			fulfillmentRequest.setMainBranch(modalId.split("-")[4]);
			fulfillmentRequest.setDeliveryOptionsRequestId(deliveryOptions.getId());
			fulfillmentRequest.setDeliveryOptionsRequest(deliveryOptions);
			fulfillmentRequest.setAutoReQuoteHasChanged(Boolean.TRUE);
			fulfillmentRequest.setShippingMethod(ShippingMethodEnum.valueOf(shippingMethodSplit));

			return deliveryOptions;
		} catch (ExecutionException | InterruptedException e) {
			//TODO: registrar erro aqui. Se der erro no requote, já era
			throw new RuntimeException("Not able to get new quote for auto requote");
		}
	}

	private void validateRequiredFieldsInAutoQuote(List<CartItemWithMode> cartItemWithModes, FulfillmentRequest fulfillmentRequest, String companyId) {
		for (CartItemWithMode item : cartItemWithModes) {
			if (item.getQuantity() == null) {
				badRequestMetrics.sendBadRequestMetrics(companyId, fulfillmentRequest.getXApplicationName(), ReasonTypeEnum.QUANTITY_IS_NULL, FulfillmentController.class.getSimpleName());
				fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
				throw new InvalidModalIdException("quantity cannot be null. item: " + item.getSku());
			}

			if (item.getStockStatus() == null) {
				badRequestMetrics.sendBadRequestMetrics(companyId, fulfillmentRequest.getXApplicationName(), ReasonTypeEnum.STOCK_STATUS_IS_NULL, FulfillmentController.class.getSimpleName());
				fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
				throw new InvalidModalIdException("stockStatus cannot be null. item: " + item.getSku());
			}

			if (item.getProductType() == null) {
				badRequestMetrics.sendBadRequestMetrics(companyId, fulfillmentRequest.getXApplicationName(), ReasonTypeEnum.PRODUCT_TYPE_IS_NULL, FulfillmentController.class.getSimpleName());
				fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
				throw new InvalidModalIdException("productType cannot be null. item: " + item.getSku());
			}

			if (item.getWeight() == null) {
				badRequestMetrics.sendBadRequestMetrics(companyId, fulfillmentRequest.getXApplicationName(), ReasonTypeEnum.MEASURES_IS_NULL, FulfillmentController.class.getSimpleName());
				fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
				throw new InvalidModalIdException("Weight cannot be null. item: " + item.getSku());
			}

			if (item.getLength() == null) {
				badRequestMetrics.sendBadRequestMetrics(companyId, fulfillmentRequest.getXApplicationName(), ReasonTypeEnum.MEASURES_IS_NULL, FulfillmentController.class.getSimpleName());
				fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
				throw new InvalidModalIdException("length cannot be null. item: " + item.getSku());
			}

			if (item.getWidth() == null) {
				badRequestMetrics.sendBadRequestMetrics(companyId, fulfillmentRequest.getXApplicationName(), ReasonTypeEnum.MEASURES_IS_NULL, FulfillmentController.class.getSimpleName());
				fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
				throw new InvalidModalIdException("Width cannot be null. item: " + item.getSku());
			}

			if (item.getHeight() == null) {
				badRequestMetrics.sendBadRequestMetrics(companyId, fulfillmentRequest.getXApplicationName(), ReasonTypeEnum.MEASURES_IS_NULL, FulfillmentController.class.getSimpleName());
				fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
				throw new InvalidModalIdException("Height cannot be null. item: " + item.getSku());
			}
		}
	}

	private DeliveryOptionsRequest preparingDataFromQuotation(FulfillmentRequest fulfillmentRequest) {

		DeliveryOptionsRequest deliveryOptions = new DeliveryOptionsRequest();
		String companyId = fulfillmentRequest.getCompanyId();
		Optional<String> xApplicationNameOptional = Optional.ofNullable(fulfillmentRequest.getXApplicationName());

		Integer combinationsTimeOut = config.getConfigValueInteger(companyId,
				xApplicationNameOptional,
				c -> c.getTimeout().getCombinations(),
				true);


		setDefaultParamsFromQuotation(deliveryOptions,
				fulfillmentRequest.getXApplicationName(),
				fulfillmentRequest.getXCurrentDate(),
				fulfillmentRequest.getXLocale(),
				fulfillmentRequest.getCompanyId());

		deliveryOptions.setShoppingCart(ShoppingCart.builder()
				.destination(CartDestination.builder().zipcode(fulfillmentRequest.getDestinationZipcode()).build())
				.items(fulfillmentRequest.getCartOrder().getItems()
						.stream()
						.map(e -> (CartItem) e)
						.collect(Collectors.toList()))
				.build());

		//TODO adicionar flag de bloqueio de quebras prioritarias em caso de autorequote;
		deliveryOptions.setQuoteSettings(QuoteSettings.builder()
				.maxOriginsHeader(1)
				.maxOriginsStoreHeader(1)
				.maxCombinationsTimeOutConfig(combinationsTimeOut)
				.branchesForShippingStrategyConfig(BranchesForShippingStrategyEnum.fromValue(
						config.getConfigValueString(companyId, xApplicationNameOptional, CompanyConfigEntity::getBranchesForShippingStrategy, true)
				))
				.eagerBranchesConfig(config.getConfigValueAsListOfString(companyId, xApplicationNameOptional, CompanyConfigEntity::getEagerBranches, false))
				.combinationApproachCartSizeLimitConfig(config.getConfigValueInteger(companyId, xApplicationNameOptional, CompanyConfigEntity::getCombinationApproachCartSizeLimit, true))
				.blockedBranches(fulfillmentRequest.getQuoteSettings().getBlockedBranches())
				.build());

		return deliveryOptions;
	}

	private List<PickupOptionsRequest> findPickupOptionsRequest(FulfillmentRequest fulfillmentRequest) throws NoQuotationAvailableForFulfillment {
		if (!fulfillmentRequest.getAutoReQuoteHasChanged()) {
			return pickupOptionsDB.findByDeliveryOptionsId(fulfillmentRequest.getCartOrder().getId())
					.stream()
					.filter(p -> p.getErrorMessage() == null)
					.filter(p -> p.getResponse() != null && p.getResponse().getPickupOptions() != null && !p.getResponse().getPickupOptions().isEmpty())
					.collect(Collectors.toList());
		} else {
			return fulfillmentRequest.getShippingMethod().equals(ShippingMethodEnum.PICKUP) ?
					Collections.singletonList(autoPickupOptionsRequest(fulfillmentRequest)) :
					new ArrayList<>();
		}
	}

	private PickupOptionsRequest autoPickupOptionsRequest(FulfillmentRequest fulfillmentRequest) throws NoQuotationAvailableForFulfillment {
		PickupOptionsRequest pickupOptionsRequest = preparingDataFromPickup(fulfillmentRequest);
		try {
			PickupOptionsReturn pickupOptionsReturn = pickupBusiness.getPickupOptions(pickupOptionsRequest, fulfillmentRequest.getDeliveryOptionsRequest(), null, null);
			pickupOptionsRequest.setResponse(pickupOptionsReturn);

			fulfillmentRequest.setPickupOptionsRequest(pickupOptionsRequest);

			Optional<PickupOption> pickupOptions = pickupOptionsReturn.getPickupOptions()
					.stream()
					.filter(pickupOption -> pickupOption.getBranchId().equals(fulfillmentRequest.getCartOrder().getItems().get(0).getBranchOfficeId().toString()))
					.findFirst();

			pickupOptions.ifPresent(fulfillmentRequest::setPickupOption);
			//TODO SELECIONAR O MELHOR PICKUP

		} catch (Exception ex) {
			fulfillmentRequest.addException("autoQuotePickupOptionsReturn", ex);
			throw new NoQuotationAvailableForFulfillment("Error auto quotation pickup", ex);
		}
		return pickupOptionsRequest;
	}

	private PickupOptionsRequest preparingDataFromPickup(FulfillmentRequest fulfillmentRequest) {
		PickupOptionsRequest pickupOptionsRequest = new PickupOptionsRequest();
		setDefaultParamsFromPickup(fulfillmentRequest, pickupOptionsRequest);
		pickupOptionsRequest.setDeliveryOptionsId(fulfillmentRequest.getDeliveryOptionsRequestId());
		pickupOptionsRequest.setZipcode(fulfillmentRequest.getDestinationZipcode());
		pickupOptionsRequest.setSkus(fulfillmentRequest.getCartOrder().getItems().stream().map(CartItem::getSku).collect(Collectors.toList()));
		pickupOptionsRequest.setQuoteSettings(QuoteSettings.builder()
				.blockedBranches(fulfillmentRequest.getQuoteSettings().getBlockedBranches())
				.eagerBranchesHeader(Collections.singletonList(fulfillmentRequest.getMainBranch()))
				.build());

		return pickupOptionsRequest;
	}

	private static void setDefaultParamsFromPickup(FulfillmentRequest fulfillmentRequest, PickupOptionsRequest pickupOptionsRequest) {
		pickupOptionsRequest.setId(UUID.randomUUID().toString());
		pickupOptionsRequest.setInitialTimestamp(System.currentTimeMillis());
		pickupOptionsRequest.setDate(LocalDateTime.now());
		if (Objects.nonNull(fulfillmentRequest.getPickupOptionsRequest())) {
			pickupOptionsRequest.setXApplicationName(fulfillmentRequest.getPickupOptionsRequest().getXApplicationName());
			pickupOptionsRequest.setXCurrentDate(fulfillmentRequest.getPickupOptionsRequest().getXCurrentDate());
			pickupOptionsRequest.setXLocale(fulfillmentRequest.getPickupOptionsRequest().getXLocale());
			pickupOptionsRequest.setCompanyId(fulfillmentRequest.getPickupOptionsRequest().getCompanyId());
		} else {
			pickupOptionsRequest.setXApplicationName(fulfillmentRequest.getXApplicationName());
			pickupOptionsRequest.setXCurrentDate(fulfillmentRequest.getXCurrentDate());
			pickupOptionsRequest.setXLocale(fulfillmentRequest.getXLocale());
			pickupOptionsRequest.setCompanyId(fulfillmentRequest.getCompanyId());
		}
	}

	private static String getFulfillmentKey(String originBranchOfficeId, ShippingMethodEnum shippingMethod, String branchOfficeId) {
		return originBranchOfficeId + "-" + shippingMethod + "-" + branchOfficeId;
	}

	private Map<String, List<CartItemWithMode>> validateSkusAndModalIds(
			FulfillmentRequest fulfillmentRequest,
			DeliveryOptionsRequest deliveryOptions,
			List<PickupOptionsRequest> pickupOptionsRequestList
	) {
		List<String> modalIdNotFound = new ArrayList<>();
		Map<String, List<CartItemWithMode>> itemMap = new HashMap<>();
		Map<String, List<CartItem>> newItemMap = new HashMap<>();

		final String ecommBranchOfficeId = ecommBusiness.getEcommBranchOffice(fulfillmentRequest.getCompanyId(), fulfillmentRequest.getXApplicationName())
				.getBranchOfficeId();

		boolean containsPreSaleItem = fulfillmentRequest.getCartOrder().getItems().stream()
				.anyMatch(c -> StockStatusEnum.isPreSale(c.getCartItem().getStockStatus()));

		if (containsPreSaleItem) {
			List<String> skus = fulfillmentRequest.getCartOrder().getItems().stream()
					.map(CartItemWithMode::getSku)
					.collect(Collectors.toList());

			newItemMap = Optional.ofNullable(deliveryOptions.getPreSaleItemBranchMap())
					.map(Map::entrySet)
					.map(preSaleItemBranchMap -> preSaleItemBranchMap.stream()
							.filter(e -> !e.getKey().startsWith(UNALLOCATED_INVENTORY + "-"))
							.filter(e -> e.getValue().stream().anyMatch(c -> skus.contains(c.getSku())))
							.map(item -> new AbstractMap.SimpleEntry<>(ecommBranchOfficeId, item.getValue()))
							.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
					.orElse(newItemMap);
		} else if (deliveryOptions.getFinalItemBranchMap() != null) {
			newItemMap = deliveryOptions.getFinalItemBranchMap().entrySet().stream()
					.filter(e -> !e.getKey().equals(UNALLOCATED_INVENTORY))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		final String freightCostCurrency = config.getConfigValueString(deliveryOptions.getCompanyId(),
				Optional.ofNullable(deliveryOptions.getXApplicationName()),
				CompanyConfigEntity::getFreightCostCurrency,
				true);

		fulfillmentRequest.getCartOrder().getItems().forEach(items -> {
			Optional<DeliveryOption> deliveryOptionForSkuOptional = deliveryOptions.getResponse()
					.getDeliveryOptions()
					.stream()
					.filter(o -> items.getSku().equals(o.getSku()))
					.findFirst();

			boolean hasItemOmniStockOnCart = items.getCartItem() != null && items.getCartItem().getStockStatus().equals(StockStatusEnum.INOMNISTOCK);
			fulfillmentRequest.setHasItemOmniStockOnCart(hasItemOmniStockOnCart);

			if (deliveryOptionForSkuOptional.isEmpty())
				return;

			DeliveryOption deliveryOption = deliveryOptionForSkuOptional.get();
			DeliveryMode deliveryMode = getDeliveryModeOptional(fulfillmentRequest, pickupOptionsRequestList, modalIdNotFound, items, deliveryOption, freightCostCurrency);

			if (deliveryMode != null) {
				final String key = getFulfillmentKey(deliveryMode.getOriginBranchOfficeId(), deliveryMode.getShippingMethod(), deliveryMode.getBranchOfficeId());
				List<CartItemWithMode> list = itemMap.computeIfAbsent(key, k -> new ArrayList<>());
				items.setDeliveryMode(deliveryMode);
				list.add(items);
			}
		});

		if (!modalIdNotFound.isEmpty()) {
			fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);

			badRequestMetrics.sendBadRequestMetrics(fulfillmentRequest.getCompanyId(),
					fulfillmentRequest.getXApplicationName(),
					ReasonTypeEnum.INVALID_MODAL_ID,
					FulfillmentBusiness.class.getSimpleName());

			throw new ModalIdNotFoundException("DeliveryOption requested and not found for modalId " + String.join(", ", modalIdNotFound));
		}

		validateSkusGroups(fulfillmentRequest, newItemMap);
		validateShippingMethod(itemMap, fulfillmentRequest);

		if (itemMap.keySet().size() != 1) {
			fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
			throw new BrokerBadRequestException("There is more than one stock origin in this fulfillment");
		}

		Optional<String> firstKeyCartItem = itemMap.keySet().stream().findFirst();

		firstKeyCartItem.ifPresent(key -> {
			fulfillmentRequest.setShippingMethod(ShippingMethodEnum.fromValue(key.split("-")[1]));

			if (fulfillmentRequest.getShippingMethod().getShippingType().equals(ShippingTypeEnum.SHIPPING))
				fulfillmentRequest.setMainBranch(key.split("-")[0]);
			else
				fulfillmentRequest.setMainBranch(key.split("-")[2]);
		});

		return itemMap;
	}

	private Map<String, List<CartItemWithMode>> linkSkusWithNewDeliveryMode(
			FulfillmentRequest fulfillmentRequest,
			DeliveryOptionsRequest deliveryOptions,
			PickupOptionsRequest pickupOptionsRequest,
			Map<String, List<CartItemWithMode>> newItemMap
	) throws NoQuotationAvailableForFulfillment {
		Map<String, List<CartItemWithMode>> itemMap = new HashMap<>();

		for (CartItemWithMode item : fulfillmentRequest.getCartOrder().getItems()) {
			if (newItemMap.get(UNALLOCATED_INVENTORY) != null
					&& newItemMap.get(UNALLOCATED_INVENTORY).stream().anyMatch(c -> c.getSku().equals(item.getSku()))) {
				itemMap.computeIfAbsent(UNAVAILABLE, k -> new ArrayList<>()).add(item);
				continue;
			}

			DeliveryOption deliveryOption = null;
			if (deliveryOptions != null && deliveryOptions.getResponse() != null && deliveryOptions.getResponse().getDeliveryOptions() != null) {
				Optional<DeliveryOption> deliveryOptionForSkuOptional = deliveryOptions.getResponse()
						.getDeliveryOptions()
						.stream()
						.filter(o -> item.getSku().equals(o.getSku()))
						.findFirst();

				if (deliveryOptionForSkuOptional.isPresent())
					deliveryOption = deliveryOptionForSkuOptional.get();
			}

			String freightCostCurrency = config.getConfigValueString(fulfillmentRequest.getCompanyId(),
					Optional.ofNullable(fulfillmentRequest.getXApplicationName()),
					CompanyConfigEntity::getFreightCostCurrency,
					true);

			Optional<DeliveryMode> deliveryModeOptional = getDeliveryModeOptional(pickupOptionsRequest, item, deliveryOption, item.getModalId(), freightCostCurrency);

			if (deliveryModeOptional.isPresent()) {
				DeliveryMode deliveryMode = deliveryModeOptional.get();
				List<CartItemWithMode> list = itemMap.computeIfAbsent(getFulfillmentKey(deliveryMode.getOriginBranchOfficeId(), deliveryMode.getShippingMethod(), deliveryMode.getBranchOfficeId()), k -> new ArrayList<>());
				item.setDeliveryMode(deliveryMode);
				list.add(item);
			} else {
				throw new NoQuotationAvailableForFulfillment("Could not find equivalent delivery option", null);
			}
		}

		return itemMap;
	}

	private void validateShippingMethod(Map<String, List<CartItemWithMode>> itemMap, FulfillmentRequest fulfillmentRequest) {
		List<ShippingMethodEnum> allShippingMethodEnums = new ArrayList<>();

		for (Map.Entry<String, List<CartItemWithMode>> cartItem : itemMap.entrySet()) {
			List<ShippingMethodEnum> shippingMethodEnums = cartItem.getValue().stream().map(c -> c.getDeliveryMode().getShippingMethod()).collect(Collectors.toList());
			allShippingMethodEnums.addAll(shippingMethodEnums);
		}

		if (!allShippingMethodEnums.stream().allMatch(allShippingMethodEnums.get(0)::equals)) {
			fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
			throw new BrokerBadRequestException("All of the shippingMethod from the partOrder must be the same.");
		}
	}

	private void validateSkusGroups(FulfillmentRequest fulfillmentRequest, Map<String, List<CartItem>> cartItemsMap) {
		List<String> skusGroups = cartItemsMap
				.values()
				.stream()
				.map(cartItems -> cartItems
						.stream()
						.map(CartItem::getSku)
						.sorted()
						.collect(Collectors.joining(","))
				)
				.collect(Collectors.toList());

		String requestedSkus = fulfillmentRequest.getCartOrder()
				.getItems()
				.stream()
				.map(CartItemWithMode::getSku)
				.sorted()
				.collect(Collectors.joining(","));

		if (!skusGroups.contains(requestedSkus)) {

			badRequestMetrics.sendBadRequestMetrics(fulfillmentRequest.getCompanyId(),
					fulfillmentRequest.getXApplicationName(),
					ReasonTypeEnum.INVALID_SKU_GROUP,
					PickupBusiness.class.getSimpleName());

			fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
			throw new BrokerBadRequestException("Sku groups available are " + skusGroups.stream()
					.map(s -> "[" + s + "]")
					.collect(Collectors.toList())
					+ ". Part Order must contain exactly one of the reported skus groups.");
		}
	}

	private Optional<DeliveryMode> getDeliveryModeOptional(
			PickupOptionsRequest pickupOptionsRequest,
			CartItemWithMode i,
			DeliveryOption deliveryOption,
			String modalId,
			String freightCostCurrency) {
		if (i.getModalId().contains("PICKUP")) {
			return getPickupDeliveryModeOptional(pickupOptionsRequest, i, freightCostCurrency);
		} else {
			return getShippingDeliveryModeOptional(i, deliveryOption, modalId);
		}
	}

	private DeliveryMode getDeliveryModeOptional(
			FulfillmentRequest fulfillmentRequest,
			List<PickupOptionsRequest> pickupOptionsRequestList,
			List<String> modalIdNotFound,
			CartItemWithMode cartItemWithMode,
			DeliveryOption deliveryOption,
			String freightCostCurrency
	) {
		DeliveryMode deliveryMode = null;

		if (cartItemWithMode.getModalId().contains("PICKUP")) {
			deliveryMode = getPickupDeliveryMode(fulfillmentRequest, pickupOptionsRequestList, cartItemWithMode, fulfillmentRequest.getCompanyId(), freightCostCurrency);
		} else {
			deliveryMode = getShippingDeliveryMode(modalIdNotFound, cartItemWithMode, deliveryOption.getDeliveryModesVerbose());
		}

		return deliveryMode;
	}

	private static DeliveryMode getShippingDeliveryMode(List<String> modalIdNotFound, CartItemWithMode cartItemWithMode, List<DeliveryMode> deliveryModesVerbose) {
		return deliveryModesVerbose
				.stream()
				.filter(m -> cartItemWithMode.getModalId().equals(m.getModalId()))
				.findFirst()
				.orElseGet(() -> {
					modalIdNotFound.add("(sku: " + cartItemWithMode.getSku() + ", modalId: " + cartItemWithMode.getModalId() + ", branchOfficeId: " + (cartItemWithMode.getBranchOfficeId() == null ? "{null}" : cartItemWithMode.getBranchOfficeId()) + ")");
					return null;
				});
	}

	private static Optional<DeliveryMode> getShippingDeliveryModeOptional(CartItemWithMode i, DeliveryOption deliveryOption, String modalId) {
		if (deliveryOption == null)
			return Optional.empty();

		String shippingMethodString = modalId.split("-")[1];
		ShippingMethodEnum shippingMethod = ShippingMethodEnum.fromValue(shippingMethodString);

		Optional<DeliveryMode> deliveryModeOptional = Optional.empty();
		for (DeliveryMode m : deliveryOption.getDeliveryModes()) {
			if (m.getIsRecommendation() != null && m.getIsRecommendation() && m.getShippingMethod() == shippingMethod) {
				deliveryModeOptional = Optional.of(m);
				break;
			}
		}

		return deliveryModeOptional;
	}

	private Optional<DeliveryMode> getPickupDeliveryModeOptional(
			PickupOptionsRequest pickupOptionsRequest,
			CartItemWithMode i,
			String freightCostCurrency
	) {
		Optional<PickupOption> chosenPickup = pickupOptionsRequest.getResponse()
				.getPickupOptions()
				.stream()
				.filter(o -> o.getBranchId().equals(i.getBranchOfficeId().toString()))
				.findFirst();

		if (chosenPickup.isEmpty())
			return Optional.empty();

		PickupOption pickupOption = chosenPickup.get();

		String description = null;
		String provider = null;
		if (pickupOption.getStockType() == DeliveryOptionsStockTypeEnum.SHIPPING_TO) {
			description = pickupOption.getQuoteDeliveryOption().getDescription();
			provider = pickupOption.getQuoteDeliveryOption().getLogisticProviderName();
		}
		return Optional.of(DeliveryMode.builder()
				.branchOfficeId(pickupOption.getBranchId())
				.originBranchOfficeId(pickupOption.getOriginBranchOfficeId())
				.deliveryMethodId(pickupOption.getDeliveryMethodId() == null ? null : Integer.parseInt(pickupOption.getDeliveryMethodId()))
				.quotationId(pickupOption.getQuotationId())
				.modalId(pickupOption.getDeliveryModeId())
				.fulfillmentMethod(pickupOption.getFulfillmentMethod())
				.shippingMethod(ShippingMethodEnum.PICKUP)
				.deliveryEstimateBusinessDays(pickupOption.getDeliveryEstimateBusinessDays())
				.estimatedDeliveryTimeValue(pickupOption.getDeliveryEstimateBusinessDays().toString())
				.estimatedDeliveryTimeUnit(pickupOption.getDeliveryTimeUnit())
				.estimatedDeliveryDate(FreightServiceV1.getDatePlusBusinessDays(pickupOption.getDeliveryEstimateBusinessDays()))
				.freightCostCurrency(freightCostCurrency)
				.logisticCarrierInfo(LogisticCarrier.builder().provider(provider).build())
				.description(description)
				.build()
		);
	}

	private DeliveryMode getPickupDeliveryMode(
			FulfillmentRequest fulfillmentRequest,
			List<PickupOptionsRequest> pickupOptionsRequestList,
			CartItemWithMode cartItemWithMode,
			String companyId,
			String freightCostCurrency
	) {
		PickupOptionsRequest pickupOptionsRequest = buildPickupOptionsRequest(pickupOptionsRequestList,cartItemWithMode, companyId, fulfillmentRequest.getXApplicationName());
		PickupOption pickupOption  = buildPickupOption(pickupOptionsRequest, cartItemWithMode, fulfillmentRequest);

		fulfillmentRequest.setPickupOptionsRequest(pickupOptionsRequest);
		fulfillmentRequest.setPickupOption(pickupOption);

		String description = null;
		String provider = null;

		if (pickupOption.getStockType() == DeliveryOptionsStockTypeEnum.SHIPPING_TO) {
			description = pickupOption.getQuoteDeliveryOption().getDescription();
			provider = pickupOption.getQuoteDeliveryOption().getLogisticProviderName();
		}
		return DeliveryMode.builder()
				.branchOfficeId(pickupOption.getBranchId())
				.originBranchOfficeId(pickupOption.getOriginBranchOfficeId())
				.deliveryMethodId(pickupOption.getDeliveryMethodId() == null ? null : Integer.parseInt(pickupOption.getDeliveryMethodId()))
				.quotationId(pickupOption.getQuotationId())
				.modalId(pickupOption.getDeliveryModeId())
				.fulfillmentMethod(pickupOption.getFulfillmentMethod())
				.shippingMethod(ShippingMethodEnum.PICKUP)
				.deliveryEstimateBusinessDays(pickupOption.getDeliveryEstimateBusinessDays())
				.estimatedDeliveryTimeValue(pickupOption.getDeliveryEstimateBusinessDays().toString())
				.estimatedDeliveryTimeUnit(pickupOption.getDeliveryTimeUnit())
				.estimatedDeliveryDate(FreightServiceV1.getDatePlusBusinessDays(pickupOption.getDeliveryEstimateBusinessDays()))
				.freightCostCurrency(freightCostCurrency)
				.logisticCarrierInfo(LogisticCarrier.builder().provider(provider).build())
				.description(description)
				.build();
	}

	private PickupOptionsRequest buildPickupOptionsRequest(final List<PickupOptionsRequest> pickupOptionsRequestList,
	                                                       final CartItemWithMode cartItemWithMode,
	                                                       final String companyId,
	                                                       final String channel) {
		return pickupOptionsRequestList.stream()
				.filter(p -> p.getSkus() == null || p.getSkus().contains(cartItemWithMode.getSku()))
				.sorted((a, b) -> (int) (b.getInitialTimestamp() - a.getInitialTimestamp()))
				.findFirst()
				.orElseThrow(() -> {
					badRequestMetrics.sendBadRequestMetrics(companyId,
							channel,
							ReasonTypeEnum.INVALID_MODAL_ID,
							FulfillmentBusiness.class.getSimpleName());

					return new SkuNotFoundException("Could not find pickup request for sku (you should call /pickup/options at least once, before calling /fulfillment)");
				});
	}

	private PickupOption buildPickupOption(final PickupOptionsRequest pickupOptionsRequest,
	                                       final CartItemWithMode cartItemWithMode,
	                                       FulfillmentRequest fulfillmentRequest) {
		return pickupOptionsRequest.getResponse()
				.getPickupOptions()
				.stream()
				.filter(p -> p.getBranchId().equals(cartItemWithMode.getBranchOfficeId() + "")
						&& p.getDeliveryModeId().equals(cartItemWithMode.getModalId()))
				.findFirst()
				.orElseThrow(() -> {
					badRequestMetrics.sendBadRequestMetrics(fulfillmentRequest.getCompanyId(),
							fulfillmentRequest.getXApplicationName(),
							ReasonTypeEnum.INVALID_MODAL_ID,
							FulfillmentBusiness.class.getSimpleName());

					fulfillmentRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
					return new ModalIdNotFoundException("Could not find pickup request for Modal Id");
				});
	}
}
