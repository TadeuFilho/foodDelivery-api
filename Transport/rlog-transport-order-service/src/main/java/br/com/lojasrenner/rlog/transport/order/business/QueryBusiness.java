package br.com.lojasrenner.rlog.transport.order.business;

import br.com.lojasrenner.rlog.transport.order.business.domain.query.BestLocationBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.BestSolutionBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.PrepareDeliveryOptionsResponseBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.SplitShoppingCartBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.model.FindBestSolution;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.model.Quotes;
import br.com.lojasrenner.rlog.transport.order.business.exception.BrokerException;
import br.com.lojasrenner.rlog.transport.order.business.exception.QuotationExpiredException;
import br.com.lojasrenner.rlog.transport.order.business.exception.QuotationNotFoundException;
import br.com.lojasrenner.rlog.transport.order.business.exception.UnknownBranchOfficeException;
import br.com.lojasrenner.rlog.transport.order.business.model.QuotationDTO;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.FreightServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.QuoteProductsRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.request.QuoteRequestV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.*;
import br.com.lojasrenner.rlog.transport.order.metrics.TimeoutMetrics;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.PickupOptionsReturn;
import brave.Tracing;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
@Log4j2
public class QueryBusiness {

	@Autowired
	private Tracing tracing;

	@Autowired
	private FreightServiceV1 freightService;

	@Autowired
	private BranchOfficeCachedServiceV1 branchOfficeService;

	@Autowired
	private GeolocationBusiness geolocationBusiness;

	@Autowired
	private StockBusiness stockBusiness;

	@Autowired
	private PickupBusiness pickupBusiness;

	@Autowired
	private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

	@Autowired
	private EcommBusiness ecommBusiness;

	@Autowired
	private ExecutorService executor;

	@Autowired
	private TimeoutMetrics timeoutMetrics;

	@Value("${bfl.expiration.quote-expiration-time:1440}")
	private Integer expirationMinutes;

	@Autowired
	private LiveConfig config;

	@Autowired
	private BestSolutionBusiness bestSolutionBusiness;

	@Autowired
	private SplitShoppingCartBusiness splitShoppingCartBusiness;

	@Autowired
	private BestLocationBusiness bestLocationBusiness;

	@Autowired
	private PrepareDeliveryOptionsResponseBusiness prepareDeliveryOptionsResponse;

	public DeliveryOptionsReturn getDeliveryModesForShoppingCart(DeliveryOptionsRequest deliveryOptionsRequest)
			throws InterruptedException, ExecutionException {
		registerTraceId(deliveryOptionsRequest);

		List<Future<QuotationDTO>> futures = executeInParallel(deliveryOptionsRequest);

		QuotationDTO quoteFromStore = null;

		if (!futures.get(0).isCancelled())
			quoteFromStore = futures.get(0).get();
		else {
			quoteFromStore = emptyQuotation(deliveryOptionsRequest.getItemsList());
			deliveryOptionsRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.TIMEOUT);
			deliveryOptionsRequest.addException("theadTimeout-quoteFromStore", new BrokerException("Timeout waiting for quoteFromStore future."));
		}

		QuotationDTO quoteFromEcomm = null;
		if (!futures.get(1).isCancelled())
			quoteFromEcomm = futures.get(1).get();
		else {
			quoteFromEcomm = emptyQuotation(deliveryOptionsRequest.getItemsList());
			deliveryOptionsRequest.addException("theadTimeout-quoteFromEcomm", new BrokerException("Timeout waiting for quoteFromEcomm future."));
		}

		QuotationDTO quoteFromPreSale = null;
		if (!futures.get(2).isCancelled()) {
			quoteFromPreSale = futures.get(2).get();
		} else {
			deliveryOptionsRequest.addException("theadTimeout-quoteFromPreSale", new BrokerException("Timeout waiting for quoteFromPreSale future."));
		}

		Quotes quotes = Quotes.builder()
				.quoteFromStore(quoteFromStore)
				.quoteFromEcomm(quoteFromEcomm)
				.quoteFromPreSale(quoteFromPreSale)
				.build();

		return prepareDeliveryOptionsResponse.buildResponse(deliveryOptionsRequest, quotes);
	}

	private List<Future<QuotationDTO>> executeInParallel(DeliveryOptionsRequest deliveryOptionsRequest) throws InterruptedException {
		List<CartItem> giftItems = deliveryOptionsRequest.getItemsList()
				.stream()
				.filter(i -> i.getProductType() == ProductTypeEnum.GIFT)
				.collect(Collectors.toList());

		if (!giftItems.isEmpty()) {
			Optional<CartItem> inStockItem = deliveryOptionsRequest.getItemsList()
					.stream()
					.filter(i -> i.getStockStatus() == StockStatusEnum.INSTOCK && i.getProductType() != ProductTypeEnum.GIFT)
					.min((a, b) -> (int) sortByValue(b, a));

			if (inStockItem.isPresent())
				inStockItem.get().setProductType(ProductTypeEnum.GIFT_GENERATOR);
			else
				giftItems.forEach(i -> i.setProductType(ProductTypeEnum.GIFT_INVALID));
		}


		List<CartItem> itemsList = deliveryOptionsRequest.getItemsList()
				.stream()
				.filter(i -> !StockStatusEnum.isPreSale(i.getStockStatus()))
				.collect(Collectors.toList());

		List<CartItem> itemListPreSale = deliveryOptionsRequest.getItemsList()
				.stream()
				.filter(i -> StockStatusEnum.isPreSale(i.getStockStatus()))
				.collect(Collectors.toList());

		Integer limit = config.getConfigValueInteger(deliveryOptionsRequest.getCompanyId(), Optional.ofNullable(deliveryOptionsRequest.getXApplicationName()), CompanyConfigEntity::getMaxCartCapacity, true);
		List<String> orderedItens = itemsList
				.stream()
				.filter((c) -> !giftItems.contains(c))
				.sorted((a, b) -> (int) sortByValue(b, a))
				.limit(limit == 0 || limit == null ? 9999 : limit).map(CartItem::getSku).collect(Collectors.toList());

		orderedItens.addAll(giftItems.stream().map(CartItem::getSku).collect(Collectors.toList()));

		deliveryOptionsRequest.setExcessItems(itemsList
				.stream()
				.filter((c) -> !orderedItens.contains(c.getSku()))
				.collect(Collectors.toList()));

		deliveryOptionsRequest.setUsedItems(orderedItens);

		List<CartItem> itemsListFinal = itemsList.stream().filter((i) -> orderedItens.contains(i.getSku())).collect(Collectors.toList());
		deliveryOptionsRequest.getShoppingCart().setItems(itemsListFinal);

		List<Callable<QuotationDTO>> calls = new ArrayList<>();

		Callable<QuotationDTO> callableStore = new Callable<QuotationDTO>() {
			@Override
			public QuotationDTO call() throws Exception {
				if (deliveryOptionsRequest.getShoppingCart().isContainsRestrictedOriginItems())
					return emptyQuotation(itemsListFinal);

				return quoteFromStore(getGeolocation(deliveryOptionsRequest), deliveryOptionsRequest, itemsListFinal);
			}
		};

		Callable<QuotationDTO> callableEcomm = new Callable<QuotationDTO>() {

			@Override
			public QuotationDTO call() throws Exception {
				try {
					QuotationDTO quoteFromEcomm = quoteFromEcomm(deliveryOptionsRequest, itemsListFinal);

					if (quoteFromEcomm.getQuoteMap() != null) {
						QuotationDTO quotationDTO = getQuotationDTO(quoteFromEcomm, deliveryOptionsRequest);
						if (quotationDTO != null) quoteFromEcomm = quotationDTO;
					}

					deliveryOptionsRequest.setItemBranchMapForEcomm(quoteFromEcomm.getItemListMap());
					deliveryOptionsRequest.setQuoteMapForEcomm(quoteFromEcomm.getQuoteMap());
					deliveryOptionsRequest.setEcommPickupOptionsReturnMap(quoteFromEcomm.getPickupOptionsReturnMap());

					return quoteFromEcomm;
				} catch (Exception e) {
					deliveryOptionsRequest.addException("quoteFromEcomm", e);
					return emptyQuotation(itemsListFinal);
				}
			}
		};

		Callable<QuotationDTO> callablePreSale = new Callable<QuotationDTO>() {
			@Override
			public QuotationDTO call() throws Exception {
				try {
					return quoteFromPreSale(deliveryOptionsRequest, itemListPreSale);
				} catch (Exception e) {
					deliveryOptionsRequest.addException("quoteFromPreSale", e);
					return emptyQuotation(itemListPreSale);
				}

			}
		};

		if (this.tracing != null && this.tracing.currentTraceContext() != null) {
			calls.add(this.tracing.currentTraceContext().wrap(callableStore));
			calls.add(this.tracing.currentTraceContext().wrap(callableEcomm));
			calls.add(this.tracing.currentTraceContext().wrap(callablePreSale));
		} else {
			calls.add(callableStore);
			calls.add(callableEcomm);
			calls.add(callablePreSale);
		}

		Integer threadTimeout = config.getConfigValueInteger(deliveryOptionsRequest.getCompanyId(),
				Optional.ofNullable(deliveryOptionsRequest.getXApplicationName()),
				c -> c.getTimeout().getThreads(),
				true);

		return executor.invokeAll(calls, threadTimeout, TimeUnit.MILLISECONDS);
	}

	private double sortByValue(CartItem a, CartItem b){
		return (a.getCostOfGoods() * a.getQuantity())*100 - (b.getCostOfGoods() * b.getQuantity())*100;
	}

	private QuotationDTO getQuotationDTO(QuotationDTO quoteFromEcomm, DeliveryOptionsRequest deliveryOptionsRequest) {
		try {
			Optional<String> key = quoteFromEcomm.getQuoteMap().keySet().stream().findFirst();

			if (key.isEmpty())
				return quoteFromEcomm;

			PickupOptionsRequest pickupOptionsRequest = new PickupOptionsRequest();
			pickupOptionsRequest.setCompanyId(deliveryOptionsRequest.getCompanyId());
			pickupOptionsRequest.setSkus(quoteFromEcomm.getItemListMap().get(key.get()).stream().map(CartItem::getSku).collect(Collectors.toList()));
			pickupOptionsRequest.setQuoteSettings(deliveryOptionsRequest.getQuoteSettings());
			pickupOptionsRequest.setXApplicationName(deliveryOptionsRequest.getXApplicationName());

			PickupOptionsReturn pickupOptions = pickupBusiness.getPickupOptions(pickupOptionsRequest, deliveryOptionsRequest, quoteFromEcomm.getItemListMap(), quoteFromEcomm);

			pickupOptionsRequest.setResponse(pickupOptions);

			quoteFromEcomm.addPickupOption(key.get(), pickupOptions);
		} catch (Exception e) {
			deliveryOptionsRequest.addException("getPickupOptions", e);
			return quoteFromEcomm;
		}
		return null;
	}

	private List<ShippingGroupResponseV1> getGeolocation(DeliveryOptionsRequest deliveryOptionsRequest) {
		try {
			return geolocationBusiness.getShippingGroups(deliveryOptionsRequest.getCompanyId(), deliveryOptionsRequest.getXApplicationName(), deliveryOptionsRequest.getDestinationZipcode(), deliveryOptionsRequest.getQuoteSettings(), deliveryOptionsRequest);
		} catch (Exception e) {
			deliveryOptionsRequest.addException("getGeolocation", e);
			return new ArrayList<>();
		}
	}

	private QuotationDTO quoteFromStore(List<ShippingGroupResponseV1> geolocationResponse, DeliveryOptionsRequest deliveryRequest, List<CartItem> itemsList) {
		try {
			if (itemsList.isEmpty())
				return emptyQuotation(itemsList);

			if (geolocationResponse.isEmpty()) {
				deliveryRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.NO_SHIPPING_GROUP);
				return emptyQuotation(itemsList);
			}

			List<String> storesInRange = new ArrayList<>();

			geolocationResponse.stream()
					.forEach(g -> g.getBranches().stream().forEach(b -> {
						if (!storesInRange.contains(b.toString()))
							storesInRange.add(b.toString());
					}));

			deliveryRequest.setStoresInRange(storesInRange);

			List<BranchOfficeEntity> activeBranchOffices = null;

			if (deliveryRequest.getQuoteSettings() != null && deliveryRequest.getQuoteSettings().getExtraBranchStatus() != null) {
				//se tivermos um extraBranchStatusForEager, temos que pegar a lista com todas as branches OK ou com status
				//igual ao que foi passado
				List<String> validStatus = new ArrayList<>();
				validStatus.addAll(deliveryRequest.getQuoteSettings().getExtraBranchStatus());
				validStatus.add("OK");
				activeBranchOffices = branchOfficeService.getActiveBranchOfficesForShipping(deliveryRequest.getCompanyId(), validStatus);

				//como esse status só vale para a eager, então mantemos somente quem está contido na eager ou quem está
				//marcado como ok
				activeBranchOffices = activeBranchOffices.stream()
						.filter(
								b -> deliveryRequest.getQuoteSettings().getEagerBranchesUsed().contains(b.getBranchOfficeId())
										|| b.getStatus().getOrder().equals("OK")
						)
						.collect(Collectors.toList());
			} else {
				activeBranchOffices = branchOfficeService.getActiveBranchOfficesForShipping(deliveryRequest.getCompanyId());
			}

			if (activeBranchOffices.isEmpty()) {
				deliveryRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.BRANCH_STATUS);
				return emptyQuotation(itemsList);
			}

			List<BranchOfficeEntity> activeStoresInRange = activeBranchOffices.stream()
					.filter(b -> storesInRange.contains(b.getBranchOfficeId()))
					.filter(b -> !deliveryRequest.getQuoteSettings().getBlockedBranches().contains(b.getBranchOfficeId()))
					.collect(Collectors.toList());

			if (activeStoresInRange.isEmpty()) {
				deliveryRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.OUT_OF_RANGE);
				return emptyQuotation(itemsList);
			}

			deliveryRequest.setActiveBranchIdsInRange(activeStoresInRange.stream().map(BranchOfficeEntity::getBranchOfficeId).collect(Collectors.toList()));

			ResponseEntity<List<LocationStockV1Response>> stockList = getStockList(deliveryRequest, itemsList, activeStoresInRange, config);

			if (stockList != null && stockList.hasBody())
				deliveryRequest.getStockResponseList().add(stockList.getBody());

			List<LocationStockV1Response> stockResponse = stockBusiness.overrideStockQuantities(itemsList, stockList, activeStoresInRange, ecommBusiness.getEcommBranchOffice(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName()));

			deliveryRequest.getStockResponseList().add(stockResponse);

			final Map<String, Integer> skuQuantityMap = itemsList.stream()
					.collect(Collectors.toMap(CartItem::getSku, CartItem::getQuantity));

			List<String> eagerBranches = new ArrayList<>();
			eagerBranches.addAll(deliveryRequest.getQuoteSettings().getEagerBranchesUsed());
			deliveryRequest.getEagerBranchesList().add(eagerBranches);

			List<LocationStockV1Response> stockResponseFiltered = stockBusiness.prepareStockResponse(stockResponse, deliveryRequest, storesInRange, skuQuantityMap, eagerBranches);

			if (stockResponseFiltered.isEmpty()) {
				deliveryRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.STOCK_UNAVAILABLE);
				return emptyQuotation(itemsList);
			}

			final var toFindBestSolution = FindBestSolution.builder()
					.geolocationResponse(geolocationResponse)
					.itemsList(itemsList)
					.stockResponseFiltered(stockResponseFiltered)
					.eagerBranches(eagerBranches)
					.skuQuantityMap(skuQuantityMap)
					.activeBranchOffices(activeBranchOffices)
					.build();

			final Map<String, List<CartItem>> bestSolution = bestSolutionBusiness
					.findBestSolution
							(deliveryRequest, toFindBestSolution);

			deliveryRequest.setItemBranchMap(bestSolution);

			if (bestSolution == null) {
				deliveryRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.STOCK_UNAVAILABLE);
				return emptyQuotation(itemsList);
			}


			Map<String, QuoteResponseV1> quoteMap = quoteFromEveryOrigin(deliveryRequest, bestSolution, activeBranchOffices);
			deliveryRequest.setQuoteMap(quoteMap);

			Map<String, PickupOptionsReturn> pickupOptionsMap = getPickupForEveryOrigin(deliveryRequest, bestSolution, null, false);
			deliveryRequest.setPickupOptionsReturnMap(pickupOptionsMap);

			return new QuotationDTO(bestSolution, quoteMap, pickupOptionsMap);
		} catch (Exception e) {
			deliveryRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.ERROR_IN_REQUEST);
			deliveryRequest.addException("quoteFromStore", e);
			return emptyQuotation(itemsList);
		}
	}

	private ResponseEntity<List<LocationStockV1Response>> getStockList(DeliveryOptionsRequest deliveryRequest, List<CartItem> itemsList, List<BranchOfficeEntity> activeStoresInRange, LiveConfig config) {
		boolean useParallelStockFetch = config.getConfigValueBoolean(deliveryRequest.getCompanyId(),
				Optional.ofNullable(deliveryRequest.getXApplicationName()),
				CompanyConfigEntity::getUseParallelStockFetch,
				false);

		if(useParallelStockFetch)
			return ResponseEntity.of(Optional.ofNullable(fetchStockParallelApproach(deliveryRequest, deliveryRequest.getCompanyId(), itemsList, activeStoresInRange)));
		else {
			return fetchStockResult(deliveryRequest, deliveryRequest.getCompanyId(), itemsList, activeStoresInRange);
		}
	}

	private QuotationDTO emptyQuotation(List<CartItem> items) {
		return new QuotationDTO(Map.of("0", items), null, null);
	}

	private List<LocationStockV1Response> fetchStockParallelApproach(DeliveryOptionsRequest deliveryRquest, String companyId, List<CartItem> items, List<BranchOfficeEntity> activeStoresInRange) {
		Map<String, List<LocationStockV1Response>> allStockFetch = new ConcurrentHashMap<>();
		items.parallelStream().forEach(item -> {
			ResponseEntity<List<LocationStockV1Response>> stockResult = fetchStockResult(deliveryRquest, companyId, Arrays.asList(item), activeStoresInRange);
			if(stockResult != null && stockResult.getStatusCode().is2xxSuccessful() && stockResult.hasBody()) {
				stockResult.getBody().stream().forEach(stock -> {
					List<LocationStockV1Response> list = allStockFetch.computeIfAbsent(stock.getBranchOfficeId(), k -> new ArrayList<LocationStockV1Response>());
					list.add(stock);
				});
			}
		});

		List<LocationStockV1Response> mergedStock = new ArrayList<>();
		for(Map.Entry<String, List<LocationStockV1Response>> entry : allStockFetch.entrySet()) {
			List<LocationStockItemV1Response> mergedItems = entry.getValue()
					.stream()
					.map(LocationStockV1Response::getItems)
					.flatMap(Collection::stream)
					.collect(Collectors.toList());
			LocationStockV1Response stock = entry.getValue().get(0);
			stock.setItems(mergedItems);
			mergedStock.add(stock);
		}
		return mergedStock.isEmpty() ? null : mergedStock;
	}

	private ResponseEntity<List<LocationStockV1Response>> fetchStockResult(DeliveryOptionsRequest deliveryRequest, String companyId, List<CartItem> items, List<BranchOfficeEntity> activeStoresInRange) {
		Map<String, Object> params = new HashMap<>();
		try {
			params.put("companyId", companyId);
			params.put("items", items.stream().map(CartItem::getSku).collect(Collectors.toList()));
			params.put("activeStoresInRange", activeStoresInRange.stream().map(BranchOfficeEntity::getBranchOfficeId).collect(Collectors.toList()));
		} catch (Exception e) {
			deliveryRequest.addException("addingStockInput", e);
		}

		deliveryRequest.getStockRequestInput().add(params);

		return stockBusiness.findStoreWithStock(companyId, deliveryRequest.getXApplicationName(), items, activeStoresInRange);
	}

	private List<GeoLocationResponseV1> emulateGeoResponse(Set<Integer> branchesFromGroup) {
		AtomicInteger counter = new AtomicInteger(0);
		return branchesFromGroup.stream()
				.map(b -> GeoLocationResponseV1.builder()
						.branchOfficeId(b.toString())
						.distance(counter.getAndAdd(1))
						.build())
				.collect(Collectors.toList());
	}

	private Map<String, PickupOptionsReturn> getPickupForEveryOrigin(
			DeliveryOptionsRequest deliveryRequest,
			Map<String, List<CartItem>> itemListMap,
			QuotationDTO quote,
			boolean isPreSale
	) {
		Map<String, PickupOptionsReturn> map = new ConcurrentHashMap<>();

		itemListMap.entrySet().parallelStream().forEach(entry -> {
			PickupOptionsRequest pickupOptionsRequest = new PickupOptionsRequest();
			pickupOptionsRequest.setCompanyId(deliveryRequest.getCompanyId());
			pickupOptionsRequest.setSkus(entry.getValue().stream().map(CartItem::getSku).collect(Collectors.toList()));
			pickupOptionsRequest.setQuoteSettings(deliveryRequest.getQuoteSettings());
			pickupOptionsRequest.setPreSale(isPreSale);
			pickupOptionsRequest.setXApplicationName(deliveryRequest.getXApplicationName());

			try {
				PickupOptionsReturn pickupOptions = pickupBusiness.getPickupOptions(pickupOptionsRequest, deliveryRequest, itemListMap, quote);

				pickupOptionsRequest.setResponse(pickupOptions);

				map.put(entry.getKey(), pickupOptions);
			} catch (Exception e) {
				deliveryRequest.addException("getPickupOptions", e);
			}
		});

		return map;
	}

	private Map<String, QuoteResponseV1> quoteFromEveryOrigin(DeliveryRequest<?> deliveryRequest, Map<String, List<CartItem>> itemListMap, List<BranchOfficeEntity> activeBranchOffices) {
		Map<String, QuoteResponseV1> map = new ConcurrentHashMap<>();

		itemListMap.entrySet().parallelStream().forEach(entry -> {
			if (entry.getValue().isEmpty() || entry.getKey().equals("0"))
				return;

			try {
				ResponseEntity<QuoteResponseV1> quoteFromStore = quoteStockFromStore(deliveryRequest.getCompanyId(),
						deliveryRequest.getXApplicationName(),
						deliveryRequest.getDestinationZipcode(),
						entry.getValue(),
						entry.getKey(),
						activeBranchOffices);

				if (quoteFromStore != null && quoteFromStore.getStatusCode().is2xxSuccessful() && quoteFromStore.getBody() != null) {
					map.put(entry.getKey(), quoteFromStore.getBody());
				} else {
					deliveryRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.INTELIPOST_ERROR);
				}
			} catch (Exception e) {
				deliveryRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.INTELIPOST_ERROR);
				Optional<BranchOfficeEntity> optionalBranchOffice = activeBranchOffices.stream()
						.filter(b -> b.getBranchOfficeId().equals(entry.getKey())).findFirst();
				optionalBranchOffice.ifPresent(branchOfficeEntity -> setBranchesWithErros(deliveryRequest, e.getMessage(), branchOfficeEntity, entry.getValue().stream().map(CartItem::getSku).collect(Collectors.toList()).toString()));
				deliveryRequest.addException("quoteFromStoreFromOrigin", e);
				List<CartItem> list = itemListMap.get("0");

				if (list == null) {
					itemListMap.put("0", new ArrayList<>());
					list = itemListMap.get("0");
				}

				list.addAll(entry.getValue());
				entry.getValue().clear();
			}
		});

		return map;
	}

	public void setBranchesWithErros(DeliveryRequest<?> deliveryOptionsRequest, String message, BranchOfficeEntity branch, String skus) {
		Optional<CountryEnum> countryEnum = Optional.ofNullable(CountryEnum.fromName(config.getConfigValueString(deliveryOptionsRequest.getCompanyId(), Optional.ofNullable(deliveryOptionsRequest.getXApplicationName()), CompanyConfigEntity::getCountry, false)));
		if (ServicesErrorRegexEnum.INTELIPOST_NO_DELIVERY_OPTIONS.isMatch(message)) {
			Optional<BranchWithError> branchWithError = deliveryOptionsRequest.getStatistics().getBranchesWithErrors().stream().filter(b -> b.getBranchId().equals(branch.getBranchOfficeId()) && b.getSkus().equals(skus)).findFirst();
			if (branchWithError.isEmpty()) {
				deliveryOptionsRequest.getStatistics().getBranchesWithErrors().add(BranchWithError.builder()
						.branchId(branch.getBranchOfficeId())
						.city(branch.getCity())
						.country(branch.getCountry())
						.state(branch.getState())
						.skus(skus)
						.zipCode(branch.getQuoteZipCode(countryEnum))
						.build());
			}

		}
	}

	public QuotationDTO quoteFromPreSale(DeliveryOptionsRequest deliveryRequest, List<CartItem> itemList) {
		BranchOfficeEntity branchOffice = ecommBusiness.getEcommBranchOffice(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName());
		deliveryRequest.setPreSaleBranchOfficeUsed(branchOffice);

		QuotationDTO preSaleQuote = quoteFromItem(deliveryRequest, branchOffice, itemList);

		deliveryRequest.setPreSaleItemBranchMap(preSaleQuote.getItemListMap());
		deliveryRequest.setPreSaleQuoteMap(preSaleQuote.getQuoteMap());
		deliveryRequest.setPreSalePickupOptionsReturnMap(preSaleQuote.getPickupOptionsReturnMap());

		return preSaleQuote;
	}

	public QuotationDTO quoteFromEcomm(DeliveryRequest<?> deliveryRequest, List<CartItem> itemsList) {
		if (itemsList.isEmpty())
			return emptyQuotation(itemsList);

		BranchOfficeEntity branchOffice = ecommBusiness.getEcommBranchOffice(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName());

		if (deliveryRequest.getQuoteSettings().getBlockedBranches().contains(branchOffice.getBranchOfficeId())) {
			return emptyQuotation(itemsList);
		}

		deliveryRequest.setEcommBranchOfficeUsed(branchOffice);

		String unavailableSkuStrategyText = config.getConfigValueString(
				deliveryRequest.getCompanyId(),
				Optional.ofNullable(deliveryRequest.getXApplicationName()),
				CompanyConfigEntity::getUnavailableSkusStrategy, true);

		if (Objects.equals(UnavailableSkuStrategyEnum.fromValue(unavailableSkuStrategyText), UnavailableSkuStrategyEnum.RETRY_MODE)) {
			return quoteFromBranch(deliveryRequest, branchOffice.getBranchOfficeId(), itemsList);
		}

		List<LocationStockV1Response> stockResponse = stockBusiness.overrideStockQuantities(itemsList, ResponseEntity.ok(new ArrayList<>()), Collections.singletonList(branchOffice), ecommBusiness.getEcommBranchOffice(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName()));

		deliveryRequest.setStockResponseForEcomm(stockResponse);

		List<GeoLocationResponseV1> emulateGeoResponse = emulateGeoResponse(Set.of(Integer.parseInt(branchOffice.getBranchOfficeId())));
		LocationStockV1Response bestLocation = bestLocationBusiness.findBestLocationGrouping
				(deliveryRequest, itemsList, Collections.singletonList(branchOffice), stockResponse, emulateGeoResponse, new ArrayList<>(), true, false, false);

		if (bestLocation == null)
			return emptyQuotation(itemsList);

		deliveryRequest.setBestLocationForEcomm(bestLocation);

		Map<String, List<CartItem>> itemListMap = splitShoppingCartBusiness.splitShoppingCartByOrigin(deliveryRequest, itemsList, stockResponse, bestLocation);

		Map<String, QuoteResponseV1> quoteMap = quoteFromEveryOrigin(deliveryRequest, itemListMap, Arrays.asList(branchOffice));

		return new QuotationDTO(itemListMap, quoteMap, null);
	}

	public QuotationDTO quoteFromItem(DeliveryOptionsRequest deliveryRequest, BranchOfficeEntity branch, List<CartItem> itemList) {
		QuotationDTO quoteFromItem = new QuotationDTO(new HashMap<>(), new HashMap<>(), new HashMap<>(), true);
		Optional<CountryEnum> countryEnum = Optional.ofNullable(CountryEnum.fromName(config.getConfigValueString(deliveryRequest.getCompanyId(), Optional.ofNullable(deliveryRequest.getXApplicationName()), CompanyConfigEntity::getCountry, false)));
		//TESTAR O QUE ACONTECE SE ALGUMA COTAÇÃO FALHAR
		itemList.forEach(item -> {
			try {
				QuoteRequestV1 request = QuoteRequestV1.builder()
						.destinationZipCode(deliveryRequest.getDestinationZipcode())
						.originZipCode(branch.getQuoteZipCode(countryEnum))
						.quotingMode(config.getConfigValueString(deliveryRequest.getCompanyId(),
								Optional.ofNullable(deliveryRequest.getXApplicationName()), CompanyConfigEntity::getDefaultQuotingMode, true))
						.products(buildProductList(Collections.singletonList(item)))
						.build();
				ResponseEntity<QuoteResponseV1> quote = freightService.getQuote(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName(), request);
				if (quote != null && quote.getStatusCode().is2xxSuccessful()) {
					quoteFromItem.getItemListMap().put(item.getSku(), Collections.singletonList(item));
					quoteFromItem.getQuoteMap().put(item.getSku(), quote.getBody());
					Map<String, PickupOptionsReturn> pickupOptionsMap = getPickupForEveryOrigin(deliveryRequest, Map.of(item.getSku(), Collections.singletonList(item)), quoteFromItem, true);
					quoteFromItem.getPickupOptionsReturnMap().put(item.getSku(), pickupOptionsMap.get(item.getSku()));
				} else {
					throw new BrokerException("Error on quoting from pre sale. Status != 200.");
				}
			} catch (Exception e) {
				setBranchesWithErros(deliveryRequest, e.getMessage(), branch, item.getSku());
				deliveryRequest.addException("quoteFromPreSale", e);
			}
		});

		return quoteFromItem;
	}

	public QuotationDTO quoteFromBranch(DeliveryRequest<?> deliveryRequest, String branchId, List<CartItem> itemList) {
		try {
			BranchOfficeEntity branchOffice = branchOfficeService.getBranchOffice(deliveryRequest.getCompanyId(), branchId);
			Optional<CountryEnum> countryEnum = Optional.ofNullable(CountryEnum.fromName(config.getConfigValueString(deliveryRequest.getCompanyId(), Optional.ofNullable(deliveryRequest.getXApplicationName()), CompanyConfigEntity::getCountry, false)));
			QuoteRequestV1 request = QuoteRequestV1.builder()
					.destinationZipCode(deliveryRequest.getDestinationZipcode())
					.originZipCode(branchOffice.getQuoteZipCode(countryEnum))
					.quotingMode(config.getConfigValueString(deliveryRequest.getCompanyId(),
							Optional.ofNullable(deliveryRequest.getXApplicationName()), CompanyConfigEntity::getDefaultQuotingMode, true))
					.products(buildProductList(itemList))
					.build();

			ResponseEntity<QuoteResponseV1> quote = freightService.getQuote(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName(), request);

			if (quote != null && quote.getStatusCode().is2xxSuccessful()) {
				if (branchId == null)
					deliveryRequest.setQuoteFromEcomm(quote.getBody());

				return new QuotationDTO(Map.of(branchOffice.getBranchOfficeId(), itemList), Map.of(branchOffice.getBranchOfficeId(), quote.getBody()), null);
			} else {
				throw new BrokerException("Error on quoting from ecomm. Status != 200.");
			}
		} catch (Exception e) {
			deliveryRequest.addException("quoteFromEcomm", e);
			return emptyQuotation(itemList);
		}
	}

	private ResponseEntity<QuoteResponseV1> quoteStockFromStore(String companyId, String channel, String destinationZipcode,
	                                                            List<? extends CartItem> items, String bestLocation, List<BranchOfficeEntity> activeBranchOfficeList) {
		Optional<BranchOfficeEntity> optionalBranchOffice = activeBranchOfficeList.stream()
				.filter(b -> b.getBranchOfficeId().equals(bestLocation)).findFirst();

		if (optionalBranchOffice.isEmpty()) {
			optionalBranchOffice = branchOfficeService.getEcommBranchOffices(companyId).stream()
					.filter(b -> b.getBranchOfficeId().equals(bestLocation)).findFirst();

			if (optionalBranchOffice.isEmpty())
				throw new UnknownBranchOfficeException("Unknown Branch Office on quoteStockFromStore: " + bestLocation, "400");
		}

		BranchOfficeEntity branchOffice = optionalBranchOffice.get();

		Optional<CountryEnum> countryEnum = Optional.ofNullable(CountryEnum.fromName(config.getConfigValueString(companyId, Optional.ofNullable(channel), CompanyConfigEntity::getCountry, false)));

		String originZipcode = branchOffice.getQuoteZipCode(countryEnum);

		return freightService.getQuote(companyId, channel,
				QuoteRequestV1.builder()
						.destinationZipCode(destinationZipcode)
						.originZipCode(originZipcode)
						.quotingMode(config.getConfigValueString(companyId, Optional.ofNullable(channel), CompanyConfigEntity::getDefaultQuotingMode, true))
						.products(buildProductList(items)).build());
	}

	protected static List<QuoteProductsRequestV1> buildProductList(List<? extends CartItem> cartItems) {
		List<QuoteProductsRequestV1> products = new ArrayList<>();

		cartItems.forEach(item -> products.add(
				QuoteProductsRequestV1.builder()
						.skuId(item.getSku())
						.costOfGoods(item.getCostOfGoods())
						.height(item.getHeight())
						.length(item.getLength())
						.productCategory(item.getProductCategory())
						.quantity(item.getQuantity())
						.weight(item.getWeight())
						.width(item.getWidth())
						.build()
				)
		);

		return products;
	}

	public DeliveryOptionsRequest getDeliveryOptionsById(GetQuotationRequest getQuotationRequest, boolean returnExpired) {
		Optional<DeliveryOptionsRequest> quote = deliveryOptionsDB.findById(getQuotationRequest.getDeliveryOptionsId());

		DeliveryOptionsRequest quotation;

		if (quote.isEmpty())
			throw new QuotationNotFoundException("Quotation not found: " + getQuotationRequest.getDeliveryOptionsId());

		quotation = quote.get();

		if (isQuotationExpired(quotation)) {
			if (returnExpired) return quotation;
			throw new QuotationExpiredException("Quotation expired: " + getQuotationRequest.getDeliveryOptionsId());
		}

		return quotation;
	}

	public boolean isQuotationExpired(DeliveryOptionsRequest request) {
		LocalDateTime expirationdate = request.getDate().plusMinutes(expirationMinutes);
		return LocalDateTime.now().isAfter(expirationdate);
	}

	private void registerTraceId(DeliveryRequest<?> brokerRequest) {
		if (tracing == null) {
			brokerRequest.setTraceId("tracing is null");
			return;
		}

		if (tracing.tracer() == null) {
			brokerRequest.setTraceId("tracing.tracer() is null");
			return;
		}

		if (tracing.tracer().currentSpan() == null) {
			brokerRequest.setTraceId("tracing.tracer().currentSpan() is null");
			return;
		}

		if (tracing.tracer().currentSpan().context() == null) {
			brokerRequest.setTraceId("tracing.tracer().currentSpan().context() is null");
			return;
		}

		if (tracing.tracer().currentSpan().context().traceIdString() == null) {
			brokerRequest.setTraceId("tracing.tracer().currentSpan().context().traceIdString() is null");
			return;
		}

		brokerRequest.setTraceId(tracing.tracer().currentSpan().context().traceIdString());
	}

}
