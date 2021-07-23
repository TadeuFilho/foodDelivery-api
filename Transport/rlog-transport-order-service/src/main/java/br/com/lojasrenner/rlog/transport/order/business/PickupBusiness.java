package br.com.lojasrenner.rlog.transport.order.business;

import br.com.lojasrenner.rlog.transport.order.business.exception.BranchOptionsNotFoundOnGeolocationException;
import br.com.lojasrenner.rlog.transport.order.business.exception.DeliveryOptionsRequestNotFoundException;
import br.com.lojasrenner.rlog.transport.order.business.exception.NoActiveBranchForPickupException;
import br.com.lojasrenner.rlog.transport.order.business.exception.NoBranchAvailableForState;
import br.com.lojasrenner.rlog.transport.order.business.model.QuotationDTO;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryMode;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.*;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.DeliveryOptionsDBInfrastructure;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.PickupOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.BranchOfficeCachedServiceV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.*;
import br.com.lojasrenner.rlog.transport.order.metrics.BadRequestMetrics;
import br.com.lojasrenner.exception.BadRequestException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.*;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Log4j2
public class PickupBusiness {

    @Autowired
    private DeliveryOptionsDBInfrastructure deliveryOptionsDB;

    @Autowired
    private GeolocationBusiness geolocationBusiness;

    @Autowired
    private StockBusiness stockBusiness;

    @Autowired
    private QueryBusiness queryBusiness;

    @Autowired
    private BranchOfficeCachedServiceV1 branchOfficeService;

    @Value("${bfl.fallback.branch-delivery-estimate:10}")
    private Integer fallbackValueForPickupDeliveryEstimate;

    @Autowired
    private EcommBusiness ecommBusiness;

    @Autowired
    private BadRequestMetrics badRequestMetrics;

    @Autowired
    private LiveConfig config;

    public PickupOptionsReturn getPickupOptions(PickupOptionsRequest pickupOptionsRequest) throws BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException {
        return getPickupOptions(pickupOptionsRequest, null, null, null);
    }

    public PickupOptionsReturn getPickupOptions(PickupOptionsRequest pickupOptionsRequest, DeliveryOptionsRequest deliveryOptionsRequest, Map<String, List<CartItem>> itemListMap, QuotationDTO quoteFromEcomm) throws BranchOptionsNotFoundOnGeolocationException, NoActiveBranchForPickupException {
        final DeliveryOptionsRequest options = getDeliveryOptionsRequest(pickupOptionsRequest, deliveryOptionsRequest);


        validateSkusParameterAndMaxOrigins(pickupOptionsRequest, options, itemListMap);

        final List<GeoLocationResponseV1> geolocationList = getBranchesByGeolocation(pickupOptionsRequest, options);
        List<String> geolocationListIds = geolocationList.stream().map(GeoLocationResponseV1::getBranchOfficeId).collect(Collectors.toList());

        if (geolocationList.isEmpty()) {
            pickupOptionsRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.OUT_OF_RANGE);
            throw new BranchOptionsNotFoundOnGeolocationException("Geolocation response is empty");
        }

        pickupOptionsRequest.setGeolocationResponse(geolocationList);

        List<BranchOfficeEntity> allBranchOffices = branchOfficeService.getActiveBranchOffices(pickupOptionsRequest.getCompanyId());

        if (allBranchOffices.isEmpty()) {
            pickupOptionsRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.BRANCH_STATUS);
            throw new NoActiveBranchForPickupException();
        }
        // TODO: Verificar se a variavel activeBranchofficesForShipping não é mais necessaria, já que não está sendo usado em nada.
        List<BranchOfficeEntity> activeBranchOfficesForShipping = getBranchesActiveForShipping(pickupOptionsRequest, options, geolocationListIds);

        List<BranchOfficeEntity> activeBranchOfficesForPickup = branchOfficeService.getActiveBranchOfficesForPickup(options.getCompanyId())
                .stream()
                .filter(b -> geolocationListIds.contains(b.getBranchOfficeId()))
                .collect(Collectors.toList());

        pickupOptionsRequest.setActiveBranchOffices(activeBranchOfficesForPickup);

        List<CartItem> requestedSkus = getRequestedCartItems(pickupOptionsRequest, options);
        pickupOptionsRequest.setRequestedCartItems(requestedSkus);

        QuotationDTO shippingToQuote = null;
        List<ShippingToResponseV1> shippingToBranches = new ArrayList<>();
        if (!pickupOptionsRequest.isPreSale() && shouldAllowShippingToForPickup(pickupOptionsRequest)) {
            if (reQuoteShippingToForPickup(pickupOptionsRequest)) {
                shippingToQuote = getShippingToQuote(pickupOptionsRequest, options, allBranchOffices, requestedSkus, shippingToBranches);
            } else if (pickupOptionsRequest.getQuoteSettings().getReQuotePickup() == null ||
                    !pickupOptionsRequest.getQuoteSettings().getReQuotePickup()) {
                shippingToQuote = getShippingToQuote(pickupOptionsRequest, options, allBranchOffices, requestedSkus, shippingToBranches);
            }
        }

        if (quoteFromEcomm == null)
            quoteFromEcomm = getQuotationDTO(pickupOptionsRequest, options, requestedSkus);
        pickupOptionsRequest.setQuoteMap(quoteFromEcomm.getQuoteMap());

        final List<String> branchesWithStock = new ArrayList<>();

        if (!quoteFromEcomm.getPreSale() && !options.getShoppingCart().isContainsRestrictedOriginItems())
            branchesWithStock.addAll(getBranchesWithStockAvailable(pickupOptionsRequest,
                    options,
                    activeBranchOfficesForPickup,
                    requestedSkus,
                    shippingToBranches,
                    activeBranchOfficesForShipping));
        else
            branchesWithStock.add(ecommBusiness.getEcommBranchOffice(pickupOptionsRequest.getCompanyId(), pickupOptionsRequest.getXApplicationName()).getBranchOfficeId());

        return preparePickupOptionsReturn(geolocationList,
                quoteFromEcomm,
                branchesWithStock,
                allBranchOffices,
                pickupOptionsRequest,
                shippingToBranches,
                shippingToQuote);
    }

    private QuotationDTO getShippingToQuote(PickupOptionsRequest pickupOptionsRequest, DeliveryOptionsRequest options, List<BranchOfficeEntity> activeBranchOffices, List<CartItem> requestedSkus, List<ShippingToResponseV1> shippingToBranches) {
        QuotationDTO finalShippingToQuote = null;
        List<ShippingToResponseV1> shippingToForBranchesHelper = geolocationBusiness.getShippingToForBranches(
                pickupOptionsRequest.getCompanyId(),
                pickupOptionsRequest.getXApplicationName(),
                activeBranchOffices);

        pickupOptionsRequest.setShippingToResponse(shippingToForBranchesHelper);
        shippingToBranches.addAll(shippingToForBranchesHelper);

        if (!shippingToBranches.isEmpty()) {
            Optional<ShippingToResponseV1> first = shippingToBranches.stream()
                    .filter(s -> s.getOriginBranches() != null && s.getOriginBranches().size() > 0 && s.getOriginBranches().get(0) != null)
                    .findFirst();

            if (first.isPresent()) {
                QuotationDTO shippingToQuote = getQuotationDTO(pickupOptionsRequest,
                        options,
                        requestedSkus,
                        first.get().getOriginBranches().get(0));

                if (shippingToQuote.getQuoteMap() != null && shippingToQuote.getQuoteMap().containsKey(first.get().getOriginBranches().get(0))) {
                    finalShippingToQuote = shippingToQuote.getQuoteMap()
                            .get(first.get().getOriginBranches().get(0))
                            .getContent()
                            .getDeliveryOptions()
                            .stream()
                            .anyMatch(d -> ShippingMethodEnum.fromValue(d.getDeliveryMethodType()) == ShippingMethodEnum.PICKUP ||
                                    ShippingMethodEnum.fromValue(d.getDeliveryMethodType()) == ShippingMethodEnum.LOCKER) ? shippingToQuote : null;
                }

                pickupOptionsRequest.setShippingToQuoteMap(shippingToQuote.getQuoteMap());
            }
        }
        return finalShippingToQuote;
    }

    private List<BranchOfficeEntity> getBranchesActiveForShipping(PickupOptionsRequest pickupOptionsRequest, DeliveryOptionsRequest options, List<String> geolocationListIds) {
        List<BranchOfficeEntity> activeBranchOfficesForShipping = null;

        if (pickupOptionsRequest.getQuoteSettings() != null && pickupOptionsRequest.getQuoteSettings().getExtraBranchStatus() != null) {
            //se tivermos um extraBranchStatusForEager, temos que pegar a lista com todas as branches OK ou com status
            //igual ao que foi passado
            List<String> validStatus = new ArrayList<>();
            validStatus.addAll(pickupOptionsRequest.getQuoteSettings().getExtraBranchStatus());
            validStatus.add("OK");
            activeBranchOfficesForShipping = branchOfficeService.getActiveBranchOfficesForShipping(pickupOptionsRequest.getCompanyId(), validStatus)
                    .stream()
                    .filter(b -> geolocationListIds.contains(b.getBranchOfficeId()))
                    .collect(Collectors.toList());

            //como esse status só vale para a eager, então mantemos somente quem está contido na eager ou quem está
            //marcado como ok
            activeBranchOfficesForShipping = activeBranchOfficesForShipping.stream()
                    .filter(
                            b -> pickupOptionsRequest.getQuoteSettings().getEagerBranchesUsed().contains(b.getBranchOfficeId())
                                    || b.getStatus().getOrder().equals("OK")
                    )
                    .collect(Collectors.toList());
        }
        else
            activeBranchOfficesForShipping = branchOfficeService.getActiveBranchOfficesForShipping(options.getCompanyId())
                    .stream()
                    .filter(b -> geolocationListIds.contains(b.getBranchOfficeId()))
                    .collect(Collectors.toList());
        return activeBranchOfficesForShipping;
    }

    private List<BranchOfficeEntity> getBranchesActiveForPickup(PickupOptionsRequest pickupOptionsRequest, DeliveryOptionsRequest options, List<String> geolocationListIds) {
        List<BranchOfficeEntity> activeBranchOfficesForPickup = null;

        if (pickupOptionsRequest.getQuoteSettings() != null && pickupOptionsRequest.getQuoteSettings().getExtraBranchStatus() != null) {
            //se tivermos um extraBranchStatusForEager, temos que pegar a lista com todas as branches OK ou com status
            //igual ao que foi passado
            List<String> validStatus = new ArrayList<>();
            validStatus.addAll(pickupOptionsRequest.getQuoteSettings().getExtraBranchStatus());
            validStatus.add("OK");
            activeBranchOfficesForPickup = branchOfficeService.getActiveBranchOfficesForPickup(pickupOptionsRequest.getCompanyId(), validStatus)
                    .stream()
                    .filter(b -> geolocationListIds.contains(b.getBranchOfficeId()))
                    .collect(Collectors.toList());

            //como esse status só vale para a eager, então mantemos somente quem está contido na eager ou quem está
            //marcado como ok
            activeBranchOfficesForPickup = activeBranchOfficesForPickup.stream()
                    .filter(
                            b -> pickupOptionsRequest.getQuoteSettings().getEagerBranchesUsed().contains(b.getBranchOfficeId())
                                    || b.getStatus().getOrder().equals("OK")
                    )
                    .collect(Collectors.toList());
        }
        else
            activeBranchOfficesForPickup = branchOfficeService.getActiveBranchOfficesForPickup(options.getCompanyId())
                .stream()
                .filter(b -> geolocationListIds.contains(b.getBranchOfficeId()))
                .collect(Collectors.toList());
        return activeBranchOfficesForPickup;
    }

    private boolean shouldAllowShippingToForPickup(PickupOptionsRequest pickupOptionsRequest) {
        Boolean value = config.getConfigValueBoolean(pickupOptionsRequest.getCompanyId(),
                Optional.ofNullable(pickupOptionsRequest.getXApplicationName()),
                c -> c.getShippingTo().getPickupActive(),
                false);
        return value != null && value.booleanValue();
    }

    private boolean reQuoteShippingToForPickup(PickupOptionsRequest pickupOptionsRequest) {
        Boolean reQuotePickup = pickupOptionsRequest.getQuoteSettings().getReQuotePickup();
        Boolean value = config.getConfigValueBoolean(pickupOptionsRequest.getCompanyId(),
                Optional.ofNullable(pickupOptionsRequest.getXApplicationName()),
                c -> c.getShippingTo().getReQuoteActive(),
                false);

        return BooleanUtils.isTrue(reQuotePickup) && BooleanUtils.isTrue(value);
    }

    private boolean shouldAddOperationalTimeOnEstimate(PickupOptionsRequest pickupOptionsRequest) {
        Boolean value = config.getConfigValueBoolean(pickupOptionsRequest.getCompanyId(),
                Optional.ofNullable(pickupOptionsRequest.getXApplicationName()),
                c -> c.getShippingTo().getAddOperationalTimeOnEstimate(),
                false);
        return value != null && value.booleanValue();
    }

    private void validateSkusParameterAndMaxOrigins(PickupOptionsRequest pickupOptionsRequest, final DeliveryOptionsRequest options, Map<String, List<CartItem>> itemMap) {
        //contingencia usando o maxOrigins do application caso não exista quoteSettings para essa cotação
        int maxOriginsUsed = config.getConfigValueInteger(pickupOptionsRequest.getCompanyId(),
                Optional.ofNullable(options.getXApplicationName()), CompanyConfigEntity::getMaxOrigins, true);
        if (options.getQuoteSettings() != null)
            maxOriginsUsed = options.getQuoteSettings().getMaxOriginsUsed();

        if (maxOriginsUsed <= 1)
            return;

        if (pickupOptionsRequest.getSkus() == null || pickupOptionsRequest.getSkus().isEmpty())
            throw new BadRequestException("Max origins is set to " + maxOriginsUsed + ". Parameter 'skus' is mandatory in this case.", "400");

        if (itemMap == null) {
            Map<String, List<CartItem>> newItemMap = new HashMap<>();

            if (options.getFinalItemBranchMap() != null)
                options.getFinalItemBranchMap().entrySet().forEach(e -> newItemMap.put(e.getKey(), e.getValue()));

            if (options.getPreSaleItemBranchMap() != null)
                options.getPreSaleItemBranchMap().entrySet().forEach(e -> newItemMap.put(e.getKey(), e.getValue()));

            itemMap = newItemMap;
        }

        if (itemMap.isEmpty())
            itemMap = Map.of(ecommBusiness.getEcommBranchOffice(pickupOptionsRequest.getCompanyId(), options.getXApplicationName()).getBranchOfficeId(), options.getShoppingCart().getItems());

        List<String> groups = itemMap
                .entrySet()
                .stream()
                .map(e -> e.getValue()
                        .stream()
                        .map(CartItem::getSku)
                        .sorted()
                        .collect(Collectors.joining(","))
                )
                .collect(Collectors.toList());

        String requestedSkus = pickupOptionsRequest.getSkus().stream().sorted().collect(Collectors.joining(","));

        if (!groups.contains(requestedSkus)) {
            badRequestMetrics.sendBadRequestMetrics(options.getCompanyId(), options.getXApplicationName(), ReasonTypeEnum.INVALID_SKU_GROUP, PickupBusiness.class.getSimpleName());
            throw new BadRequestException("Sku groups available are " + groups.stream()
                    .map(s -> "[" + s + "]")
                    .collect(Collectors.toList())
                    + ". Parameter skus should contain exactly one of these sequences.", "400");
        }
    }

    private boolean containsEcommItem(DeliveryOptionsReturn response, List<CartItem> requestedSkus) {
        List<String> skus = requestedSkus.stream().map(CartItem::getSku).collect(Collectors.toList());

        return response.getDeliveryOptions()
                .stream()
                .filter(d -> skus.contains(d.getSku()))
                .anyMatch(d -> d.getDeliveryModes()
                        .stream()
                        .anyMatch(m -> m.getShippingMethod() == ShippingMethodEnum.PICKUP
                                &&  FulfillmentMethodEnum.CD.isMatch(m.getFulfillmentMethod())));
    }

    private List<CartItem> getRequestedCartItems(PickupOptionsRequest pickupOptionsRequest, DeliveryOptionsRequest options) {
        List<CartItem> requestedSkus = new ArrayList<>();

        requestedSkus.addAll(options.getItemsList()
                .stream()
                .filter(i -> pickupOptionsRequest.getSkus() == null || pickupOptionsRequest.getSkus().isEmpty() || pickupOptionsRequest.getSkus().contains(i.getSku()))
                .collect(Collectors.toList()));

        return requestedSkus;
    }

    private DeliveryOptionsRequest getDeliveryOptionsRequest(PickupOptionsRequest pickupOptionsRequest,
                                                             DeliveryOptionsRequest deliveryOptionsRequest) {
        if (deliveryOptionsRequest == null) {
            Optional<DeliveryOptionsRequest> optionalDeliveryOptions = deliveryOptionsDB.findById(pickupOptionsRequest.getDeliveryOptionsId());

            if (optionalDeliveryOptions.isEmpty())
                throw new DeliveryOptionsRequestNotFoundException("DeliveryOptionsRequest not found for ID: " + pickupOptionsRequest.getDeliveryOptionsId());

            deliveryOptionsRequest = optionalDeliveryOptions.get();
        }

        return deliveryOptionsRequest;
    }

    private List<String> getBranchesWithStockAvailable(
            PickupOptionsRequest pickupOptionsRequest,
            final DeliveryOptionsRequest options,
            List<BranchOfficeEntity> activeBranchOfficesForPickup,
            List<CartItem> requestedSkus,
            List<ShippingToResponseV1> shippingToBranches,
            List<BranchOfficeEntity> activeBranchOfficesForShipping
    ) {
        final List<LocationStockV1Response> locationsStockResponse = new ArrayList<>();

        List<String> activeForShipping = activeBranchOfficesForShipping.stream()
                .map(BranchOfficeEntity::getBranchOfficeId)
                .collect(Collectors.toList());

        ResponseEntity<List<LocationStockV1Response>> stockResponse = null;

        List<String> activeBranchesOfficesJoined = new ArrayList<>();

        try {
            //se tiver item de CD, nao precisa verificar estoque. exibimos tudo com estoque de CD
            if (shouldAvoidFulfillmentTypeMixing(options, requestedSkus)) {
                activeBranchOfficesForPickup.clear();
                activeForShipping.clear();
            }

            activeBranchOfficesForPickup.add(ecommBusiness.getEcommBranchOffice(pickupOptionsRequest.getCompanyId(), pickupOptionsRequest.getXApplicationName()));
            activeBranchesOfficesJoined.addAll(activeBranchOfficesForPickup.stream().map(BranchOfficeEntity::getBranchOfficeId).collect(Collectors.toList()));
            for (ShippingToResponseV1 shippingBranches : shippingToBranches) {
                //apenas branches com doShipping podem oferecer shippingTo
                activeBranchesOfficesJoined.addAll(shippingBranches.getOriginBranches()
                        .stream()
                        .filter(activeForShipping::contains)
                        .collect(Collectors.toList()));
            }
            activeBranchesOfficesJoined = activeBranchesOfficesJoined.stream().distinct().collect(Collectors.toList());
            stockResponse = stockBusiness.findStoreWithStockWithString(options.getCompanyId(),
                    options.getXApplicationName(),
                    requestedSkus,
                    activeBranchesOfficesJoined);
        } catch (Exception e) {
            pickupOptionsRequest.addException("checkStock", e);
        }

        List<LocationStockV1Response> stockList = stockBusiness.overrideStockQuantities(requestedSkus, stockResponse, activeBranchOfficesForPickup, ecommBusiness.getEcommBranchOffice(pickupOptionsRequest.getCompanyId(), pickupOptionsRequest.getXApplicationName()));
        locationsStockResponse.addAll(stockList);
        pickupOptionsRequest.setStockResponse(stockList);

        List<String> branchesWithStock = getBranchesWithStock(activeBranchesOfficesJoined, locationsStockResponse, requestedSkus, pickupOptionsRequest);

        pickupOptionsRequest.setBranchesWithStock(branchesWithStock);

        return branchesWithStock;
    }

    private boolean shouldAvoidFulfillmentTypeMixing(DeliveryOptionsRequest options, List<CartItem> requestedSkus) {
        Boolean avoidFulfillmentTypeMixingForCD = config.getConfigValueBoolean(options.getCompanyId(),
                Optional.ofNullable(options.getXApplicationName()),
                c -> c.getPickup().getAvoidFulfillmentTypeMixingForCD(),
                false);

        if (options.getShoppingCart().isContainsRestrictedOriginItems())
            return true;
        else return options.getResponse() != null && containsEcommItem(options.getResponse(), requestedSkus) && avoidFulfillmentTypeMixingForCD;
    }

    private QuotationDTO getQuotationDTO(
            PickupOptionsRequest pickupOptionsRequest,
            final DeliveryOptionsRequest options,
            List<CartItem> requestedSkus
    ) {
        return getQuotationDTO(pickupOptionsRequest, options, requestedSkus, null);
    }

    private QuotationDTO getQuotationDTO(
            PickupOptionsRequest pickupOptionsRequest,
            final DeliveryOptionsRequest options,
            List<CartItem> requestedSkus,
            String branchId
    ) {
        //TODO: Não conseguimos encontrar um fluxo que entre nesse If
        if (branchId == null && pickupOptionsRequest.getState() == null && (pickupOptionsRequest.getSkus() == null || pickupOptionsRequest.getSkus().isEmpty())) {
            return new QuotationDTO(Map.of(ecommBusiness.getEcommBranchOffice(pickupOptionsRequest.getCompanyId(), pickupOptionsRequest.getXApplicationName()).getBranchOfficeId(), options.getItemsList()), Map.of(ecommBusiness.getEcommBranchOffice(pickupOptionsRequest.getCompanyId(), pickupOptionsRequest.getXApplicationName()).getBranchOfficeId(), options.getQuoteFromEcomm()), null);
        }

        CartDestination destination = options.getShoppingCart().getDestination();

        if (pickupOptionsRequest.getState() != null) {
            List<BranchOfficeEntity> allBranches = branchOfficeService.getBranchOffices(pickupOptionsRequest.getCompanyId());
            List<BranchOfficeEntity> branchesWithinTheState = allBranches.stream()
                    .filter(b -> pickupOptionsRequest.getState().equalsIgnoreCase(b.getState()))
                    .collect(Collectors.toList());

            if (branchesWithinTheState.isEmpty())
                throw new NoBranchAvailableForState("At least one branch is needed to quote from ecomm to the state.");

            destination.setZipcode(branchesWithinTheState.get(0).getZipcode());
        }

        //quoteFromEcomm so com os itens solicitados
        DeliveryOptionsRequest newRequest = new DeliveryOptionsRequest();
        newRequest.setId(options.getId());
        newRequest.setCompanyId(options.getCompanyId());
        newRequest.setXApplicationName(options.getXApplicationName());
        newRequest.setShoppingCart(ShoppingCart.builder()
                .destination(destination)
                .containsRestrictedOriginItems(options.getShoppingCart().isContainsRestrictedOriginItems())
                .extraIdentification(options.getShoppingCart().getExtraIdentification())
                .items(requestedSkus)
                .build());
        newRequest.setQuoteSettings(options.getQuoteSettings());

        if (requestedSkus.stream().anyMatch(c -> StockStatusEnum.isPreSale(c.getStockStatus()))) {
            List<CartItem> itemList = newRequest.getItemsList().stream().filter(i -> StockStatusEnum.isPreSale(i.getStockStatus())).collect(Collectors.toList());
            return queryBusiness.quoteFromPreSale(newRequest, itemList);
        } else if (branchId != null) {
            List<CartItem> itemList = newRequest.getItemsList().stream().filter(i -> !StockStatusEnum.isPreSale(i.getStockStatus())).collect(Collectors.toList());
            return queryBusiness.quoteFromBranch(newRequest, branchId, itemList);
        } else {
            List<CartItem> itemList = newRequest.getItemsList().stream().filter(i -> !StockStatusEnum.isPreSale(i.getStockStatus())).collect(Collectors.toList());
            return queryBusiness.quoteFromEcomm(newRequest, itemList);
        }
    }

    private PickupOptionsReturn preparePickupOptionsReturn(
            final List<GeoLocationResponseV1> geolocationList,
            final QuotationDTO quoteFromEcomm,
            List<String> branchesWithStock,
            List<BranchOfficeEntity> allBranchOffices,
            PickupOptionsRequest pickupOptionsRequest,
            List<ShippingToResponseV1> shippingToBranches,
            final QuotationDTO shippingToQuote
    ) {
        QuoteResponseV1 quoteResponseV1 = null;

        if (quoteFromEcomm.getQuoteMap() != null && !quoteFromEcomm.getPreSale())
            quoteResponseV1 = quoteFromEcomm.getQuoteMap().get(ecommBusiness.getEcommBranchOffice(pickupOptionsRequest.getCompanyId(), pickupOptionsRequest.getXApplicationName()).getBranchOfficeId());
        else if (quoteFromEcomm.getQuoteMap() != null && quoteFromEcomm.getPreSale()) {
            quoteResponseV1 = quoteFromEcomm.getQuoteMap().get(pickupOptionsRequest.getSkus().get(0));
        }
        QuoteResponseV1 finalQuoteResponseV1 = quoteResponseV1;

        QuoteResponseV1 shippingToQuoteResponseV1 = null;

        if (shippingToQuote != null && shippingToQuote.getQuoteMap() != null) {
            Optional<String> first = shippingToQuote.getQuoteMap().keySet().stream().filter(k -> !k.equals("0")).findFirst();

            if (first.isPresent())
                shippingToQuoteResponseV1 = shippingToQuote.getQuoteMap().get(first.get());
        }
        QuoteResponseV1 finalShippingToQuoteResponseV1 = shippingToQuoteResponseV1;

        List<PickupOption> pickupOptions = geolocationList.stream()
                .map(geoResponse -> getDataForPickupOption(branchesWithStock, allBranchOffices, geoResponse, finalQuoteResponseV1, pickupOptionsRequest, shippingToBranches, finalShippingToQuoteResponseV1))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new PickupOptionsReturn(pickupOptions
                .stream()
                .sorted(Comparator.comparing(PickupOption::getDeliveryEstimateBusinessDays)
                        .thenComparing(PickupOption::getDistance)
                        .thenComparing(PickupOption::getBranchId))
                .collect(Collectors.toList()));
    }

    private PickupOption getDataForPickupOption(
            List<String> branchesWithStock,
            List<BranchOfficeEntity> allBranchOffices,
            GeoLocationResponseV1 geoResponse,
            QuoteResponseV1 quoteResponse,
            PickupOptionsRequest pickupOptionsRequest,
            List<ShippingToResponseV1> shippingToResponseList,
            QuoteResponseV1 finalShippingToQuoteResponseV1
    ) {
        Optional<BranchOfficeEntity> branch = allBranchOffices.stream()
                .filter(b -> b.getBranchOfficeId().equals(geoResponse.getBranchOfficeId()))
                .findFirst();

        if (branch.isEmpty()) {
            if (pickupOptionsRequest.getQuoteSettings().getEagerBranchesUsed().size() == 1 && geoResponse.getBranchOfficeId().equals(pickupOptionsRequest.getQuoteSettings().getEagerBranchesUsed().get(0)))
                pickupOptionsRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.BRANCH_STATUS);
            return null;
        }

        String branchFulfillmentMethod = branch.get().getConfiguration().getCdManagement();
        String fulfillmentMethod = null;
        String bestShippingStore = null;
        Optional<BranchOfficeEntity> bestShippingStoreEntity = Optional.empty();
        Integer shippingToFreightTime = null;

        boolean branchWithdraw = branch.get().getConfiguration() != null
                && branch.get().getConfiguration().getPermission() != null
                && branch.get().getConfiguration().getPermission().getBranchWithdraw() != null
                && branch.get().getConfiguration().getPermission().getBranchWithdraw();

        DeliveryOptionsStockTypeEnum stockType = null;
        if (branchesWithStock.contains(geoResponse.getBranchOfficeId()) && branchWithdraw) {
            stockType = DeliveryOptionsStockTypeEnum.OWN_STOCK;
            fulfillmentMethod = branchFulfillmentMethod;
        }
        else {
            List<ShippingToResponseV1> shippingToResponse = shippingToResponseList
                    .stream()
                    .filter((i) -> geoResponse.getBranchOfficeId().equals(i.getDestinationBranch()))
                    .collect(Collectors.toList());

            if(shippingToResponse.isEmpty() || finalShippingToQuoteResponseV1 == null)
                fulfillmentMethod = checkIfEcommIsAvailable(pickupOptionsRequest, branchesWithStock);
			else {
				bestShippingStore = checkAlternativeShippingToBranch(shippingToResponse.stream()
						.map(ShippingToResponseV1::getOriginBranches)
						.flatMap(Collection::stream)
						.collect(Collectors.toList()), branchesWithStock);

				String finalBestShippingStore = bestShippingStore;

                bestShippingStoreEntity = allBranchOffices.stream()
                        .filter(i -> i.getBranchOfficeId().equals(finalBestShippingStore))
                        .findFirst();

				branchFulfillmentMethod = bestShippingStoreEntity
                        .map(b -> b.getConfiguration().getCdManagement())
                        .orElse(FulfillmentMethodEnum.STORE.getValue());

				Optional<ShippingToResponseV1> shippingToBranch = shippingToResponse.stream().filter(shippingTo -> shippingTo.getOriginBranches().contains(finalBestShippingStore)).findFirst();
				shippingToFreightTime = shippingToBranch.map(ShippingToResponseV1::getFreightTime).orElse(null);
				fulfillmentMethod = bestShippingStore != null ? branchFulfillmentMethod : checkIfEcommIsAvailable(pickupOptionsRequest, branchesWithStock);
				stockType = bestShippingStore != null ? DeliveryOptionsStockTypeEnum.SHIPPING_TO : null;
			}

        }

        if (fulfillmentMethod != null && stockType == null)
            stockType = DeliveryOptionsStockTypeEnum.CD;

        //se ta null, é pq essa branch nao tem estoque proprio e o CD tbm nao tem
        if (fulfillmentMethod == null) {
            if (pickupOptionsRequest.getQuoteSettings().getEagerBranchesUsed().size() == 1 && branch.get().getBranchOfficeId().equals(pickupOptionsRequest.getQuoteSettings().getEagerBranchesUsed().get(0)))
                pickupOptionsRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.STOCK_UNAVAILABLE);
            return null;
        }

        int deliveryTime = 0;
        QuoteDeliveryOptionsResponseV1 deliveryOption = null;

        if (bestShippingStore != null && finalShippingToQuoteResponseV1 != null) {
            if (bestShippingStoreEntity.isEmpty()) {
                if (pickupOptionsRequest.getQuoteSettings().getEagerBranchesUsed().size() == 1 && branch.get().getBranchOfficeId().equals(pickupOptionsRequest.getQuoteSettings().getEagerBranchesUsed().get(0)))
                    pickupOptionsRequest.getStatistics().setReason(DeliveryOptionsErrorEnum.BRANCH_STATUS);
                return null;
            }

            //int storeWithdrawalTerm = branch.get().getConfiguration().getStoreWithdrawalTerm() == null ? 0 : branch.get().getConfiguration().getStoreWithdrawalTerm().intValue();
            int shippingToStoreWithdrawalTerm = bestShippingStoreEntity.get().getConfiguration().getStoreWithdrawalTerm() == null ? 0 : bestShippingStoreEntity.get().getConfiguration().getStoreWithdrawalTerm().intValue();

            if (shippingToFreightTime == null)
                deliveryTime = getDeliveryEstimateForPickup(branch.get(), fulfillmentMethod, finalShippingToQuoteResponseV1, true);
            else
                deliveryTime = shippingToFreightTime;

            if (shouldAddOperationalTimeOnEstimate(pickupOptionsRequest))
                deliveryTime += shippingToStoreWithdrawalTerm;

            deliveryOption = getDeliveryOptionForPickup(branch.get(), fulfillmentMethod, finalShippingToQuoteResponseV1, true);
        } else {
            deliveryTime = getDeliveryEstimateForPickup(branch.get(), fulfillmentMethod, quoteResponse, false);
            deliveryOption = getDeliveryOptionForPickup(branch.get(), fulfillmentMethod, quoteResponse, false);
        }

        if (deliveryOption == null && FulfillmentMethodEnum.CD.isMatch(fulfillmentMethod))
            return null;
        if (FulfillmentMethodEnum.CD.isMatch(fulfillmentMethod) && !branch.get().getConfiguration().getPermission().getBranchWithdrawStockCD())
            return null;

        Long quoteId = null;

        if (quoteResponse != null && FulfillmentMethodEnum.CD.isMatch(fulfillmentMethod)) {
            quoteId = quoteResponse.getContent().getId();
        } else if (finalShippingToQuoteResponseV1 != null && FulfillmentMethodEnum.STORE.isMatch(fulfillmentMethod)) {
            quoteId = finalShippingToQuoteResponseV1.getContent().getId();
        }

        return mapPickupOption(geoResponse, branch.get(), fulfillmentMethod, deliveryTime, quoteId, deliveryOption, pickupOptionsRequest, bestShippingStore, stockType);
    }

    private String checkAlternativeShippingToBranch(List<String> shippingToBranches, List<String> branchesWithStock){
        Optional<String> branch = shippingToBranches.stream().filter(branchesWithStock::contains).findFirst();
        return branch.orElse(null);
    }

    private String checkIfEcommIsAvailable(PickupOptionsRequest pickupOptionsRequest, List<String> branchesWithStock) {
        BranchOfficeEntity ecomm = ecommBusiness.getEcommBranchOffice(pickupOptionsRequest.getCompanyId(), pickupOptionsRequest.getXApplicationName());
        String ecomFulfillmentMethod = ecomm.getConfiguration().getCdManagement();
        return branchesWithStock.contains(ecomm.getBranchOfficeId()) ? ecomFulfillmentMethod : null;
    }

    private PickupOption mapPickupOption(GeoLocationResponseV1 geoResponse, BranchOfficeEntity branch, String fulfillmentMethod, int deliveryTime, Long quotationId, QuoteDeliveryOptionsResponseV1 deliveryMethod, PickupOptionsRequest pickupOptionsRequest, String bestShippingStore, DeliveryOptionsStockTypeEnum stockType) {
        String deliveryMethodId = deliveryMethod == null || deliveryMethod.getDeliveryMethodId() == null ? null : deliveryMethod.getDeliveryMethodId() + "";
        BranchOfficeEntity originBranchOffice = null;
        if(bestShippingStore != null)
            originBranchOffice = branchOfficeService.getBranchOffice(pickupOptionsRequest.getCompanyId(), bestShippingStore);
        else
            originBranchOffice = FulfillmentMethodEnum.STORE.isMatch(fulfillmentMethod) ? branch : ecommBusiness.getEcommBranchOffice(pickupOptionsRequest.getCompanyId(), pickupOptionsRequest.getXApplicationName());

        return PickupOption.builder()
                .branchId(branch.getBranchOfficeId())
                .originBranchOfficeId(originBranchOffice.getBranchOfficeId())
                .branchType(BranchTypeEnum.STORE)
                .deliveryEstimateBusinessDays(deliveryTime)
                .deliveryModeId(DeliveryMode.generateModalId(fulfillmentMethod, ShippingMethodEnum.PICKUP, deliveryTime, 0.0, originBranchOffice.getBranchOfficeId()))
                .deliveryTimeUnit(TimeUnityEnum.DAY)
                .deliveryTime(deliveryTime + "")
                .distance((double) geoResponse.getDistance())
                .name(branch.getName())
                .fulfillmentMethod(fulfillmentMethod)
                .quotationId(FulfillmentMethodEnum.STORE.isMatch(fulfillmentMethod) && bestShippingStore == null ? null : quotationId)
                .deliveryMethodId(FulfillmentMethodEnum.STORE.isMatch(fulfillmentMethod) && bestShippingStore == null ? null : deliveryMethodId)
                .stockType(stockType)
                .quoteDeliveryOption(deliveryMethod)
                .state(originBranchOffice.getState())
                .build();
    }

    private List<String> getBranchesWithStock(
            List<String> activeBranchOfficesForPickupInRangeOrShippingTo,
            final List<LocationStockV1Response> locationsStockResponse,
            List<CartItem> items,
            PickupOptionsRequest request
    ) {
        Map<String, Integer> skuQuantityMap = items.stream().collect(Collectors.toMap(CartItem::getSku, CartItem::getQuantity));

        List<LocationStockV1Response> locationsWithStock = stockBusiness.filterAndSortLocations(items, locationsStockResponse, activeBranchOfficesForPickupInRangeOrShippingTo, new ArrayList<>(), request)
                .stream()
                .filter(f -> !request.getQuoteSettings().getBlockedBranches().contains(f.getBranchOfficeId()))
                .filter(f ->
                        // tem que conter todos os itens
                        f.getItems() != null && f.getItems().size() == skuQuantityMap.size()
                                && f.getItems().stream().allMatch(i ->
                                // todos os itens tem que estar não bloqueados
                                !i.isBlocked()
                                        // e tem que ter quantidade igual ou superior a solicitada
                                        && i.getAmountSaleable() >= skuQuantityMap.get(i.getSku())))
                .collect(Collectors.toList());

        return locationsWithStock.stream().map(LocationStockV1Response::getBranchOfficeId).collect(Collectors.toList());
    }

    private List<GeoLocationResponseV1> getBranchesByGeolocation(PickupOptionsRequest pickupOptionsRequest, DeliveryOptionsRequest options) {
        final List<GeoLocationResponseV1> geolocationList = new ArrayList<>();

        if (pickupOptionsRequest.getState() != null) {
            geolocationList.addAll(geolocationBusiness.getGeolocationResponseForState(options.getCompanyId(), options.getXApplicationName(), pickupOptionsRequest.getState()));
        } else {
            if (pickupOptionsRequest.getZipcode() != null)
                options.getShoppingCart().getDestination().setZipcode(pickupOptionsRequest.getZipcode());

            geolocationList.addAll(geolocationBusiness.getBranchesForPickup(options.getCompanyId(), options.getXApplicationName(), options.getDestinationZipcode()));
        }
        return geolocationList;
    }

    private int getDeliveryEstimateForPickup(BranchOfficeEntity branchOfficeForPickup, String fulfillmentMethod, QuoteResponseV1 quote, boolean shippingTo) {
        int deliveryEstimateBusinessDays = Integer.MAX_VALUE;
        if (FulfillmentMethodEnum.STORE.isMatch(fulfillmentMethod) && !shippingTo) {
            //simplesmente usa o que ta configurado no branchOffice

            if (branchOfficeForPickup.getConfiguration().getStoreWithdrawalTerm() != null)
                deliveryEstimateBusinessDays = branchOfficeForPickup.getConfiguration().getStoreWithdrawalTerm();
            else {
                deliveryEstimateBusinessDays = fallbackValueForPickupDeliveryEstimate;
            }
        } else if (quote != null) {
            //foi feita uma cotação nesse estado
            //vamos usar o prazo daquele pickup
            List<QuoteDeliveryOptionsResponseV1> pickupFromCDList = quote
                    .getContent()
                    .getDeliveryOptions()
                    .stream()
                    .filter(d -> ShippingMethodEnum.fromValue(d.getDeliveryMethodType()) == ShippingMethodEnum.PICKUP || ShippingMethodEnum.fromValue(d.getDeliveryMethodType()) == ShippingMethodEnum.LOCKER)
                    .collect(Collectors.toList());

            List<QuoteDeliveryOptionsResponseV1> pickupLockerCDList = pickupFromCDList.stream()
                    .filter(d -> equals(d.getLogisticProviderName(), branchOfficeForPickup.getConfiguration().getShippingCompanyLocker()))
                    .collect(Collectors.toList());

            if (!pickupLockerCDList.isEmpty()) {
                deliveryEstimateBusinessDays = pickupLockerCDList.get(0).getDeliveryEstimateBusinessDays();
            } else if (!pickupFromCDList.isEmpty()) {
                deliveryEstimateBusinessDays = pickupFromCDList.get(0).getDeliveryEstimateBusinessDays();
            }

        }

        return deliveryEstimateBusinessDays;
    }

    private QuoteDeliveryOptionsResponseV1 getDeliveryOptionForPickup(BranchOfficeEntity branchOffice, String fulfillmentMethod, QuoteResponseV1 quote, boolean shippingTo) {
        if ((FulfillmentMethodEnum.CD.isMatch(fulfillmentMethod) || shippingTo) && quote != null) {
            //foi feita uma cotação nesse estado
            //vamos usar o prazo daquele pickup
            List<QuoteDeliveryOptionsResponseV1> pickupFromCDList = quote
                    .getContent()
                    .getDeliveryOptions()
                    .stream()
                    .filter(d -> ShippingMethodEnum.fromValue(d.getDeliveryMethodType()) == ShippingMethodEnum.PICKUP || ShippingMethodEnum.fromValue(d.getDeliveryMethodType()) == ShippingMethodEnum.LOCKER)
                    .collect(Collectors.toList());

            List<QuoteDeliveryOptionsResponseV1> pickupLockerCDList = pickupFromCDList.stream()
                    .filter(d -> equals(d.getLogisticProviderName(), branchOffice.getConfiguration().getShippingCompanyLocker()))
                    .collect(Collectors.toList());

            if (!pickupLockerCDList.isEmpty()) {
                return pickupLockerCDList.get(0);
            } else if (!pickupFromCDList.isEmpty()) {
                return pickupFromCDList.get(0);
            }
        }

        return null;
    }

    private static boolean equals(String logisticProviderName, String shippingCompanyLocker) {
        if (logisticProviderName == null && shippingCompanyLocker == null)
            return true;

        if (logisticProviderName != null && shippingCompanyLocker != null)
            return logisticProviderName.trim().equalsIgnoreCase(shippingCompanyLocker.trim());

        return false;
    }

}
