package br.com.lojasrenner.rlog.transport.order.business.domain.query;

import br.com.lojasrenner.rlog.transport.order.business.EcommBusiness;
import br.com.lojasrenner.rlog.transport.order.business.StockBusiness;
import br.com.lojasrenner.rlog.transport.order.business.domain.query.model.FindBestSolution;
import br.com.lojasrenner.rlog.transport.order.business.model.DistinctOriginDTO;
import br.com.lojasrenner.rlog.transport.order.business.util.QueryUtil;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.StockStatusEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.MetricsService;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity.BranchOfficeEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.GeoLocationResponseV1;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.model.response.LocationStockV1Response;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class BestSolutionBusiness {

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private EcommBusiness ecommBusiness;

    @Autowired
    private LiveConfig config;

    @Autowired
    private StockBusiness stockBusiness;

    @Autowired
    private BestLocationBusiness bestLocationBusiness;

    @Autowired
    private SplitShoppingCartBusiness splitShoppingCartBusiness;

    public Map<String, List<CartItem>> findBestSolution(DeliveryOptionsRequest deliveryRequest, FindBestSolution toFindBestSolution) {

        List<String> eagerBranches = toFindBestSolution.getEagerBranches();
        final List<LocationStockV1Response> stockResponseFiltered = toFindBestSolution.getStockResponseFiltered();
        final Map<String, Integer> skuQuantityMap = toFindBestSolution.getSkuQuantityMap();
        final List<BranchOfficeEntity> activeBranchOffices = toFindBestSolution.getActiveBranchOffices();

        Set<Integer> branchesFromGroup = new LinkedHashSet<>();
        Map<String, List<CartItem>> bestSolution = null;
        Map<String, List<CartItem>> lastSolution = null;

        boolean isPriorityGroup = false;
        for (int i = 0; i < toFindBestSolution.getGeolocationResponse().size(); i++) {

            deliveryRequest.getStatistics().incrementCounter("geolocation-group-loop");
            branchesFromGroup.addAll(toFindBestSolution.getGeolocationResponse().get(i).getBranches());

            boolean isExtraGrouping = false;
            if(toFindBestSolution.getGeolocationResponse().get(i).getStatePriority() != null && toFindBestSolution.getGeolocationResponse().get(i).getStatePriority() && groupPriorityIsEnable(deliveryRequest)) {
                isPriorityGroup = true;
                List<String> extraEagerBranches = toFindBestSolution.getGeolocationResponse()
                        .get(i)
                        .getBranches()
                        .stream()
                        .map(Object::toString)
                        .collect(Collectors.toList());
                deliveryRequest.getDeliveryRequestDetails().setEagerBranchesAdded(extraEagerBranches);
                eagerBranches = Stream.concat(eagerBranches.stream(), extraEagerBranches.stream())
                        .collect(Collectors.toList());

                deliveryRequest.getStatistics().setPassedOnPriorityGroup(true);
            }else if(isPriorityGroup) {
                isExtraGrouping = true;
                deliveryRequest.getStatistics().setPassedOnExtraGroup(true);
            }

            //Repetir um desses processos abaixo cada vez que for adicionada mais lojas no grupo atual.toFindBestSolution.getGeolocationResponse().get(i).getBranches()
            if (toFindBestSolution.getItemsList().size() > deliveryRequest.getQuoteSettings().getCombinationApproachCartSizeLimitUsed()) {
                deliveryRequest.getStatistics().incrementCounter("skuQuantityApproach");
                lastSolution = skuQuantityApproach(deliveryRequest,
                        eagerBranches,
                        stockResponseFiltered,
                        branchesFromGroup,
                        bestSolution,
                        skuQuantityMap,
                        isPriorityGroup,
                        isExtraGrouping);
            } else {
                deliveryRequest.getStatistics().incrementCounter("combinationApproach");
                lastSolution = combinationApproach(deliveryRequest,
                        eagerBranches,
                        stockResponseFiltered,
                        branchesFromGroup,
                        bestSolution,
                        skuQuantityMap,
                        activeBranchOffices,
                        isPriorityGroup,
                        isExtraGrouping);
            }

            if (lastSolution != null && !lastSolution.isEmpty() && compareSolutions(bestSolution, lastSolution) > 0) {
                    bestSolution = lastSolution;
            }

            if (metricsService.combinationTimeoutExceeded(deliveryRequest, true) &&
                    (lastSolution != null && (!lastSolution.containsKey("0") || lastSolution.get("0") == null || lastSolution.get("0").isEmpty())))
                break;

            if (bestSolution != null) {
                Set<CartItem> itemsOk = bestSolution.entrySet().stream()
                        .filter(e -> !e.getKey().equals("0"))
                        .map(Map.Entry::getValue)
                        .flatMap(List::stream)
                        .collect(Collectors.toSet());

                if (itemsOk.size() == skuQuantityMap.size())
                    break;
            }
        }
        return bestSolution;
    }

    private int compareSolutions(Map<String, List<CartItem>> bestSolution, Map<String, List<CartItem>> lastSolution) {
        int bestSolutionSkuCount = bestSolution == null ? 0 : bestSolution.entrySet().stream()
                .filter(v -> !v.getKey().equals("0"))
                .map(Map.Entry::getValue)
                .mapToInt(List::size)
                .sum();

         int lastSolutionSkuCount = lastSolution == null ? 0 : lastSolution.entrySet().stream()
                 .filter(v -> !v.getKey().equals("0"))
                 .map(Map.Entry::getValue)
                 .mapToInt(List::size)
                 .sum();

        int skuCountDiff = lastSolutionSkuCount - bestSolutionSkuCount;

        if (skuCountDiff != 0)
            return skuCountDiff;

        int bestSolutionStoreCount = bestSolution == null ? 0 : (int) bestSolution.keySet().stream().filter(k -> !k.equals("0")).count();
        int lastSolutionStoreCount = lastSolution == null ? 0 : (int) lastSolution.keySet().stream().filter(k -> !k.equals("0")).count();

        return lastSolutionStoreCount - bestSolutionStoreCount;
    }

    private Map<String, List<CartItem>> skuQuantityApproach(DeliveryOptionsRequest deliveryRequest,
                                                            List<String> eagerBranches,
                                                            List<LocationStockV1Response> stockResponseFiltered,
                                                            Set<Integer> branchesFromGroup,
                                                            Map<String, List<CartItem>> bestSolution,
                                                            Map<String, Integer> skuQuantityMap,
                                                            boolean isPriorityGroup,
                                                            boolean isExtraGroup) {
        //ordenar as lojas com as que atendem a maior quantidade de items na frente.
        //remover da lista ordenada as lojas que nao pertencem ao grupo atual.
        List<LocationStockV1Response> stockResponseSorted = stockResponseFiltered.stream()
                .sorted((a, b) -> b.getOkCount() - a.getOkCount())
                .filter(stock -> branchesFromGroup.contains(Integer.valueOf(stock.getBranchOfficeId())))
                .collect(Collectors.toList());

        List<CartItem> remainder = deliveryRequest.getItemsList().stream()
                .filter(i -> !StockStatusEnum.isPreSale(i.getStockStatus()))
                .collect(Collectors.toList());

        List<String> alreadyVisitedStore = new ArrayList<>();

        Map<String, List<CartItem>> lastSolution = new ConcurrentHashMap<>();

        DistinctOriginDTO distinctOrigins = DistinctOriginDTO.builder()
                .maxOrigins(deliveryRequest.getQuoteSettings().getMaxOriginsUsed() + (!isPriorityGroup ? 0 : getMaxOriginsPriority(deliveryRequest)) + (!isExtraGroup ? 0 : getMaxOriginsPriorityExtra(deliveryRequest)))
                .maxStoreOrigin(deliveryRequest.getQuoteSettings().getMaxOriginsStoreUsed() + (!isPriorityGroup ? 0 : getMaxOriginsPriority(deliveryRequest)) + (!isExtraGroup ? 0 : getMaxOriginsPriorityExtra(deliveryRequest)))
                .build();

        final List<String> ecommBranchIds = ecommBusiness.getAllEcommBranchOffices(deliveryRequest.getCompanyId(), deliveryRequest.getXApplicationName())
                .stream()
                .map(BranchOfficeEntity::getBranchOfficeId)
                .collect(Collectors.toList());

        //repetir o mesmo processo até a quantidade de quebras não for atingida e remainder não esteja vazio, ou quando não possui mais lojas com items do remainder.
        while (distinctOrigins.getMaxOrigins() > 0 && !remainder.isEmpty()) {
            deliveryRequest.getStatistics().incrementCounter("skuQuantityApproach-while");
            //ordernar a loja que atendem a maior quantidade de items remainder na frente.
            // depois ordenar as lojas eager para ficar na frente de todas outras e ter maior prioridade.
            Optional<LocationStockV1Response> stockResponseCleaned = stockResponseSorted.stream()
                    .filter(stock -> !alreadyVisitedStore.contains(stock.getBranchOfficeId()))
                    .filter(stock -> checkIfStoreHaveTheItemsOfRemainder(remainder, stock))
                    .sorted((a, b) -> b.getRemainderOkItems().size() - a.getRemainderOkItems().size())
                    .sorted((a, b) -> QueryUtil.sortByEagerBranches(a, b, eagerBranches))
                    .findFirst();

            stockResponseCleaned.ifPresent(bestStore -> {
                //pegar a loja que atende a maior quantidade de items.

                // Separar qual items essa loja atende
                List<CartItem> cartItemsInStockOfBestStore = remainder.stream()
                        .filter(item -> bestStore.getOkItems().contains(item.getSku()))
                        .collect(Collectors.toList());

                if (!cartItemsInStockOfBestStore.isEmpty()) {
                    //Adicionar a loja e seus itens ao lastSolution se existir items no cartItemsInStockByStore.
                    //Validar a quantidade de quebras somente quando o item é adiconado com sucesso
                    //remover os items que foram atendido da variavel remainder somente quando eles são adicionados com sucesso.
                    if (distinctOrigins.getMaxOrigins() > 0 &&
                            ecommBranchIds.contains(bestStore.getBranchOfficeId()) ||
                            distinctOrigins.getMaxStoreOrigin() > 0) {
                        lastSolution.put(bestStore.getBranchOfficeId(), cartItemsInStockOfBestStore);
                        checkDistinctOrigins(bestStore.getBranchOfficeId(), distinctOrigins, ecommBranchIds);
                        remainder.removeAll(cartItemsInStockOfBestStore);
                    }
                    alreadyVisitedStore.add(bestStore.getBranchOfficeId());
                }
            });

            if (metricsService.combinationTimeoutExceeded(deliveryRequest) || stockResponseCleaned.isEmpty()) {
                break;
            }
        }

        List<String> eagerOrigins = lastSolution.keySet()
                .stream()
                .filter(eagerBranches::contains)
                .collect(Collectors.toList());

        //guardar o remainder que não foi capaz de ser atendido nessa solução na posicao 0 do bestSolution
        if (remainder != null && !remainder.isEmpty())
            lastSolution.put("0", remainder);
        //guardar a melhor solução dessa iteração na variavel bestSolution
        if (bestSolution == null || countItemsAvailable(lastSolution, skuQuantityMap, eagerOrigins) > countItemsAvailable(bestSolution, skuQuantityMap, eagerOrigins))
            return lastSolution;

        return null;
    }

    private Map<String, List<CartItem>> combinationApproach(
            DeliveryOptionsRequest deliveryRequest,
            List<String> eagerBranches,
            List<LocationStockV1Response> stockResponseFiltered,
            Set<Integer> branchesFromGroup,
            Map<String, List<CartItem>> bestSolution,
            Map<String, Integer> skuQuantityMap,
            List<BranchOfficeEntity> activeBranchOffices,
            boolean isPriorityGroup,
            boolean isExtraGrouping
    ) {
        Map<String, List<CartItem>> lastSolution;
        LocationStockV1Response bestLocation;
        List<CartItem> itemList = deliveryRequest.getItemsList().stream()
                .filter(i -> !StockStatusEnum.isPreSale(i.getStockStatus()))
                .collect(Collectors.toList());

        List<BranchOfficeEntity> activeBranchOfficesEager = activeBranchOffices.stream()
                .filter(b -> eagerBranches.contains(b.getBranchOfficeId()))
                .collect(Collectors.toList());
        LocationStockV1Response eagerBestLocation = bestLocationBusiness.findBestLocationGrouping(deliveryRequest, itemList, activeBranchOfficesEager, stockResponseFiltered, emulateGeoResponse(branchesFromGroup), null, isPriorityGroup, isExtraGrouping);

        Map<String, List<CartItem>> eagerSolution = (eagerBestLocation == null) ? Map.of("0", itemList)
                : splitShoppingCartBusiness.splitShoppingCartByOrigin(deliveryRequest, itemList, stockResponseFiltered, eagerBestLocation);

        List<BranchOfficeEntity> activeBranchOfficesRegular = activeBranchOffices.stream()
                .filter(b -> !eagerBranches.contains(b.getBranchOfficeId()))
                .collect(Collectors.toList());

        List<CartItem> remainder = eagerSolution.get("0");

        List<String> eagerOrigins = eagerSolution.keySet()
                .stream()
                .filter(k -> !k.equals("0"))
                .collect(Collectors.toList());

        if (remainder != null && !remainder.isEmpty()) {
            bestLocation = bestLocationBusiness.findBestLocationGrouping(deliveryRequest, remainder, activeBranchOfficesRegular, stockResponseFiltered, emulateGeoResponse(branchesFromGroup), eagerOrigins, isPriorityGroup, isExtraGrouping);

            if (bestLocation == null){
                if(!eagerSolution.isEmpty())
                    return eagerSolution;
                else
                    return null;
            }else
                lastSolution = splitShoppingCartBusiness.splitShoppingCartByOrigin(deliveryRequest, remainder, stockResponseFiltered, bestLocation);
        } else {
            lastSolution = eagerSolution;
        }

        eagerSolution.entrySet().forEach(entry -> {
            if (!entry.getKey().equals("0"))
                lastSolution.put(entry.getKey(), entry.getValue());
        });

        if (!lastSolution.containsKey("0") || lastSolution.get("0") == null || lastSolution.get("0").isEmpty()) {
            return lastSolution;
        }

        if (bestSolution == null || countItemsAvailable(lastSolution, skuQuantityMap, eagerOrigins) > countItemsAvailable(bestSolution, skuQuantityMap, eagerOrigins))
            return lastSolution;


        return lastSolution;
    }

    private void checkDistinctOrigins(String branchOfficeId, DistinctOriginDTO distinctOrigins, final List<String> ecommBranchs) {
        Integer eagerEcommOrigins = (ecommBranchs.contains(branchOfficeId)) ? 1 : 0;
        Integer eagerStoreOrigins = (ecommBranchs.contains(branchOfficeId)) ? 0 : 1;

        distinctOrigins.setMaxOrigins(distinctOrigins.getMaxOrigins() - eagerEcommOrigins - eagerStoreOrigins);
        distinctOrigins.setMaxStoreOrigin(distinctOrigins.getMaxStoreOrigin() - eagerStoreOrigins);
    }

    private Boolean checkIfStoreHaveTheItemsOfRemainder(List<CartItem> remainder, LocationStockV1Response stock) {
        List<String> existentItem = new ArrayList<>();

        for (CartItem item : remainder) {
            if (stock.getOkItems().contains(item.getSku()))
                existentItem.add(item.getSku());
        }
        if (!existentItem.isEmpty()) {
            stock.setRemainderOkItems(existentItem);
            return true;
        }
        return false;
    }

    private int countItemsAvailable(Map<String, List<CartItem>> itemMap, Map<String, Integer> skuQuantityMap, List<String> eagerOrigins) {
        if (itemMap == null || itemMap.isEmpty())
            return 0;

        return skuQuantityMap.entrySet()
                .stream()
                .mapToInt(sku ->
                        itemMap.entrySet().stream().mapToInt(entry -> {
                            Optional<CartItem> optionalItem = entry.getValue().stream().filter(i -> i.getSku().equals(sku.getKey())).findAny();
                            if (optionalItem.isEmpty() || entry.getKey().equals("0"))
                                return 0;
                            else if (eagerOrigins.contains(entry.getKey()))
                                return 1000;
                            else
                                return 1;
                        })
                                .sum()
                )
                .sum();
    }

    //TODO: não deve ficar aqui - codigo repetido
    private List<GeoLocationResponseV1> emulateGeoResponse(Set<Integer> branchesFromGroup) {
        AtomicInteger counter = new AtomicInteger(0);
        return branchesFromGroup.stream()
                .map(b -> GeoLocationResponseV1.builder()
                        .branchOfficeId(b.toString())
                        .distance(counter.getAndAdd(1))
                        .build())
                .collect(Collectors.toList());
    }

    private boolean groupPriorityIsEnable(DeliveryOptionsRequest deliveryOptionsRequest){
        return config.getConfigValueBoolean(deliveryOptionsRequest.getCompanyId()
                , Optional.ofNullable(deliveryOptionsRequest.getXApplicationName()),
                c -> c.getGroupConfig().getEnableGroupPriority(),true);
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
