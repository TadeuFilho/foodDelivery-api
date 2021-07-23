package br.com.lojasrenner.rlog.transport.order.business.domain.query;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.StockBusiness;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.MetricsService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockItemV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BestLocationBusiness {

    @Autowired
    private EcommBusiness ecommBusiness;

    @Autowired
    private StockBusiness stockBusiness;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private LiveConfig config;

    public LocationStockV1Response findBestLocationGrouping(DeliveryRequest<?> deliveryRequest,
                                                             List<CartItem> itemList,
                                                             List<BranchOfficeEntity> activeBranchOffices,
                                                             List<LocationStockV1Response> stockResponse,
                                                             List<GeoLocationResponseV1> geolocationResponse,
                                                             List<String> eagerOrigins, boolean isPriorityGroup, boolean isExtraGrouping
    ) {
        return findBestLocationGrouping(deliveryRequest, itemList, activeBranchOffices, stockResponse, geolocationResponse, eagerOrigins, false, isPriorityGroup, isExtraGrouping);
    }

    public LocationStockV1Response findBestLocationGrouping(DeliveryRequest<?> deliveryRequest,
                                                             List<CartItem> itemList,
                                                             List<BranchOfficeEntity> activeBranchOffices,
                                                             List<LocationStockV1Response> stockResponse,
                                                             List<GeoLocationResponseV1> geolocationResponse,
                                                             List<String> eagerOrigins,
                                                             boolean isQuoteFromEcomm,
                                                             boolean isPriorityGroup,
                                                             boolean isExtraGrouping
    ) {
        deliveryRequest.getStatistics().incrementCounter("findBestLocationGrouping");

        List<String> activeBranchesList = activeBranchOffices.stream().map(BranchOfficeEntity::getBranchOfficeId).collect(Collectors.toList());
        List<String> branchesInGroup = geolocationResponse.stream().map(GeoLocationResponseV1::getBranchOfficeId).collect(Collectors.toList());

        Set<LocationStockV1Response> locationsSet = stockResponse.stream()
                .filter(s -> activeBranchesList.contains(s.getBranchOfficeId()) && branchesInGroup.contains(s.getBranchOfficeId()))
                .collect(Collectors.toSet());

        LocationStockV1Response bestLocation;
        LocationStockV1Response partialLocation = null;

        Map<String, Integer> skuQuantityMap = itemList.stream()
                .collect(Collectors.toMap(CartItem::getSku, CartItem::getQuantity));

        int eagerEcommOrigins = 0;
        int eagerStoreOrigins = 0;

        if (eagerOrigins != null) {
            eagerEcommOrigins = (int) eagerOrigins.stream().filter(k -> k.equals(ecommBusiness.getEcommBranchOffice(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName()).getBranchOfficeId())).count();
            eagerStoreOrigins = (int) eagerOrigins.stream().filter(k -> !k.equals(ecommBusiness.getEcommBranchOffice(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName()).getBranchOfficeId())).count();
        }

        int availableEcommOrigins = deliveryRequest.getQuoteSettings().getMaxOriginsUsed() - eagerEcommOrigins - eagerStoreOrigins + (!isPriorityGroup ? 0 : getMaxOriginsPriority(deliveryRequest)) + (!isExtraGrouping ? 0 : getMaxOriginsPriorityExtra(deliveryRequest));
        int availableStoreOrigins = deliveryRequest.getQuoteSettings().getMaxOriginsStoreUsed() - eagerStoreOrigins + (!isPriorityGroup ? 0 : getMaxOriginsPriority(deliveryRequest)) + (!isExtraGrouping ? 0 : getMaxOriginsPriorityExtra(deliveryRequest));

        final boolean thereIsOnlyCD = availableStoreOrigins == 0 && availableEcommOrigins > 0;
        for (int groupSize = 1; groupSize <= availableEcommOrigins && groupSize <= availableStoreOrigins + ((availableEcommOrigins > availableStoreOrigins) ? 1 : 0); groupSize++) {
            deliveryRequest.getStatistics().incrementCounter("findBestLocationGrouping-internal");

            //o tamanho do agrupamento (par, trio, quadra) nao pode ser maior que a quantidade de lojas disponiveis
            Set<LocationStockV1Response> filtered = locationsSet
                    .stream()
                    .filter(l -> !thereIsOnlyCD || ecommBusiness.getEcommBranchOffice(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName()).getBranchOfficeId().equals(l.getBranchOfficeId()))
                    .collect(Collectors.toSet());

            if (groupSize > filtered.size() || groupSize > itemList.size() || filtered.isEmpty())
                continue;

            Set<Set<LocationStockV1Response>> locationsCombinations = Sets.combinations(filtered, groupSize);

            List<LocationStockV1Response> groupedLocations = locationsCombinations.stream()
                    .map(combination -> combineLocationItems(deliveryRequest, combination))
                    //se ja checamos essa combinação, nao vamos mais checar para evitar duplicidade
                    .filter(l -> isQuoteFromEcomm ||
                            eagerOrigins == null ||
                            deliveryRequest.getStatistics().checkAndAddIfItIsTheFirstTimeAttemptingThisCombination(
                                    joinBranchIds(l.getBranchOfficeId(), eagerOrigins)
                            )).collect(Collectors.toList());

            List<String> activeBranchOfficesIds = activeBranchOffices.stream().map(BranchOfficeEntity::getBranchOfficeId).collect(Collectors.toList());

            LocationStockV1Response location = stockBusiness.findBestLocation(
                    itemList,
                    groupedLocations,
                    activeBranchOfficesIds,
                    geolocationResponse,
                    deliveryRequest
            );

            if (countStockAvailable(location, skuQuantityMap) == skuQuantityMap.size()
                    && (groupSize <= availableStoreOrigins || (containsEcomm(location, deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName()) && groupSize <= availableEcommOrigins))) {
                bestLocation = location;
                if (bestLocation != null)
                    return bestLocation;
            } else if ((groupSize <= availableStoreOrigins || (containsEcomm(location, deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName()) && groupSize <= availableEcommOrigins)) &&
                    (partialLocation == null || countStockAvailable(location, skuQuantityMap) > countStockAvailable(partialLocation, skuQuantityMap))) {
                partialLocation = location;
            }
            if (metricsService.combinationTimeoutExceeded(deliveryRequest)) return partialLocation;
        }

        return partialLocation;
    }

    private LocationStockV1Response combineLocationItems(DeliveryRequest<?> deliveryRequest, Set<LocationStockV1Response> combination) {
        String combinedId = String.join("+", combination.stream().map(LocationStockV1Response::getBranchOfficeId).sorted().collect(Collectors.toList()));
        String combinedStatus = combination.stream().allMatch(l -> l.getBranchOfficeStatus().equals("OK")) ? "OK" : "NOK";

        List<LocationStockItemV1Response> combinedItems = new ArrayList<>();

        for (LocationStockV1Response location : combination) {
            location.getItems().forEach(item -> {
                if (item.getBlocked() == null || !item.getBlocked()) {
                    Optional<LocationStockItemV1Response> optionalItemFromCombined = combinedItems.stream()
                            .filter(c -> c.getSku().equals(item.getSku()))
                            .findFirst();

                    if (optionalItemFromCombined.isEmpty()) {
                        combinedItems.add(beginLocationItem(item));
                    } else {
                        mergeLocationItem(item, optionalItemFromCombined.get());
                    }
                }
            });
        }

        return LocationStockV1Response.builder()
                .branchOfficeId(combinedId)
                .branchOfficeStatus(combinedStatus)
                .companyId(deliveryRequest.getCompanyId())
                .items(combinedItems)
                .build();
    }

    private String joinBranchIds(String branchOfficeId, List<String> eagerOrigins) {
        String[] regularOrigins = branchOfficeId.split("\\+");

        List<String> ids = new ArrayList<>();
        ids.addAll(eagerOrigins);
        ids.addAll(Arrays.asList(regularOrigins));
        return ids.stream().sorted().collect(Collectors.joining("+"));
    }

    private int countStockAvailable(LocationStockV1Response bestLocation, Map<String, Integer> skuQuantityMap) {
        if (bestLocation == null || bestLocation.getItems() == null || bestLocation.getItems().isEmpty())
            return 0;

        return skuQuantityMap.entrySet()
                .stream()
                .mapToInt(sku -> {
                    Optional<LocationStockItemV1Response> locationItemOptional = bestLocation.getItems().stream().filter(i -> sku.getKey().equals(i.getSku())).findFirst();

                    if (locationItemOptional.isEmpty() || locationItemOptional.get().isBlocked() || locationItemOptional.get().getAmountSaleable() == null)
                        return 0;

                    return locationItemOptional.get().getAmountSaleable() >= sku.getValue() ? 1 : 0;
                })
                .sum();
    }

    private boolean containsEcomm(LocationStockV1Response location, String companyId, String channel) {
        if (location == null || location.getBranchOfficeId() == null)
            return false;

        String branchOfficeCombination = location.getBranchOfficeId();

        String[] parts = branchOfficeCombination.split("\\+");

        for (String part : parts)
            if (ecommBusiness.getAllEcommBranchOffices(companyId, channel).stream().anyMatch((e) -> e.getBranchOfficeId().equals(part)))
                return true;

        return false;
    }

    private LocationStockItemV1Response beginLocationItem(LocationStockItemV1Response item) {
        Integer amountSaleable = item.getAmountSaleable();

        if (amountSaleable == null)
            amountSaleable = 0;

        return LocationStockItemV1Response.builder()
                .sku(item.getSku())
                .amountSaleable(amountSaleable)
                .build();
    }

    private void mergeLocationItem(LocationStockItemV1Response item, LocationStockItemV1Response itemFromCombined) {
        Integer amountSaleableFromCombined = itemFromCombined.getAmountSaleable();
        Integer amountSaleable = item.getAmountSaleable();

        if (amountSaleableFromCombined == null)
            amountSaleableFromCombined = 0;

        if (amountSaleable == null)
            amountSaleable = 0;

        if (amountSaleableFromCombined < amountSaleable)
            itemFromCombined.setAmountSaleable(amountSaleable);
    }

    private int getMaxOriginsPriority(DeliveryRequest<?> deliveryOptionsRequest){
        return config.getConfigValueInteger(deliveryOptionsRequest.getCompanyId()
                , Optional.ofNullable(deliveryOptionsRequest.getXApplicationName()),
                c -> c.getGroupConfig().getMaxOriginsPriority(),true);
    }

    private int getMaxOriginsPriorityExtra(DeliveryRequest<?> deliveryOptionsRequest){
        return config.getConfigValueInteger(deliveryOptionsRequest.getCompanyId()
                , Optional.ofNullable(deliveryOptionsRequest.getXApplicationName()),
                c -> c.getGroupConfig().getMaxOriginsPriorityExtra(),true);
    }


}
