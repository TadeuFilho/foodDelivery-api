package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1;

import br.com.lojasrenner.rlog.transport.order.business.QueryBusiness;
import br.com.lojasrenner.rlog.transport.order.configuration.LiveConfig;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.response.DeliveryOptionsReturn;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.BranchesForShippingStrategyEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.ReasonTypeEnum;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.CompanyConfigEntity;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.DeliveryOptionsRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.GetQuotationRequest;
import br.com.lojasrenner.rlog.transport.order.infrastructure.mongo.entity.QuoteSettings;
import br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.util.UpdateBrokerRequestParams;
import br.com.lojasrenner.exception.BadRequestException;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.QueryController;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.CartItem;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.controller.model.request.ShoppingCart;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
@Log4j2
public class QueryService implements UpdateBrokerRequestParams {

    @Autowired
    private QueryBusiness business;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private LiveConfig config;

    @Value("${bfl.timeout.combinations:5000}")
    private Integer combinationsTimeOut;

    @Autowired
    private HandleResponse handleResponse;

    public DeliveryOptionsReturn deliveryModesQueryForShoppingCart(String xApplicationName,
                                                                   String xCurrentDate,
                                                                   String xLocale,
                                                                   Integer xMaxOrigins,
                                                                   Integer xMaxOriginsStore,
                                                                   BranchesForShippingStrategyEnum xBranchesForShippingStrategyEnum,
                                                                   List<String> xEagerBranches,
                                                                   Integer xCombinationsTimeOut,
                                                                   Integer xCombinationApproachCartSizeLimit,
                                                                   String companyId,
                                                                   List<String> blockedBranches,
                                                                   boolean verbose,
                                                                   boolean logisticInfo,
                                                                   ShoppingCart body
    ) throws ExecutionException, InterruptedException {
        DeliveryOptionsRequest deliveryOptionsRequest = new DeliveryOptionsRequest();
        try {
            Optional<String> xApplicationNameOptional = Optional.ofNullable(xApplicationName);
            setDefaultParams(xApplicationName, xCurrentDate, xLocale, companyId, deliveryOptionsRequest);
            deliveryOptionsRequest.setVerbose(verbose);
            deliveryOptionsRequest.setLogisticInfo(logisticInfo);
            deliveryOptionsRequest.setShoppingCart(body);
            deliveryOptionsRequest.setShoppingCartOriginal(new ShoppingCart(body));
            deliveryOptionsRequest.setQuoteSettings(QuoteSettings.builder()
                    .maxOriginsHeader(xMaxOrigins)
                    .maxOriginsConfig(config.getConfigValueInteger(companyId, xApplicationNameOptional, CompanyConfigEntity::getMaxOrigins, true))
                    .maxOriginsStoreHeader(xMaxOriginsStore)
                    .maxOriginsStoreConfig(config.getConfigValueInteger(companyId, xApplicationNameOptional, CompanyConfigEntity::getMaxOriginsStore, true))
                    .maxCombinationsTimeOutHeader(xCombinationsTimeOut)
                    .maxCombinationsTimeOutConfig(combinationsTimeOut)
                    .branchesForShippingStrategyHeader(xBranchesForShippingStrategyEnum)
                    .branchesForShippingStrategyConfig(BranchesForShippingStrategyEnum.fromValue(
                            config.getConfigValueString(companyId, xApplicationNameOptional, CompanyConfigEntity::getBranchesForShippingStrategy, true)
                    ))
                    .eagerBranchesHeader(xEagerBranches)
                    .eagerBranchesConfig(config.getConfigValueAsListOfString(companyId, xApplicationNameOptional, CompanyConfigEntity::getEagerBranches, false))
                    .combinationApproachCartSizeLimitHeader(xCombinationApproachCartSizeLimit)
                    .combinationApproachCartSizeLimitConfig(config.getConfigValueInteger(companyId, xApplicationNameOptional, CompanyConfigEntity::getCombinationApproachCartSizeLimit, true))
                    .blockedBranches(blockedBranches)
                    .build());

            log.info("delivery options request start: {}", deliveryOptionsRequest);

            validate(body, companyId, xApplicationName);

            DeliveryOptionsReturn deliveryOptionsReturn = business.getDeliveryModesForShoppingCart(deliveryOptionsRequest);
            deliveryOptionsRequest.setResponse(deliveryOptionsReturn);

            return deliveryOptionsReturn;
        } catch (Exception e) {
            deliveryOptionsRequest.setErrorMessage(e.toString());
            deliveryOptionsRequest.setErrorStack(deliveryOptionsRequest.stackToString(e.getStackTrace()));
            throw e;
        } finally {
            boolean runAsync = false;
            try {
                runAsync = config.getConfigValueBoolean(companyId, Optional.ofNullable(xApplicationName), CompanyConfigEntity::getExecuteAsyncQueryResponseHandle, false);
            } catch (Exception ignored) {
                //ignored
            }

            if (runAsync) {
                handleResponse.handleQueryResponse(deliveryOptionsRequest, companyId, xApplicationName);
            } else {
                handleResponse.handleQueryResponseSync(deliveryOptionsRequest, companyId, xApplicationName);
            }
        }
    }

    public DeliveryOptionsReturn getDeliveryModesQueryById(
            String xApplicationName,
            String xCurrentDate,
            String xLocale,
            String companyId,
            String id
    ) {
        GetQuotationRequest getQuotationRequest = new GetQuotationRequest();
        setDefaultParams(xApplicationName, xCurrentDate, xLocale, companyId, getQuotationRequest);
        getQuotationRequest.setDeliveryOptionsId(id);
        return business.getDeliveryOptionsById(getQuotationRequest, false).getResponse();
    }

    private void validate(ShoppingCart shoppingCart, String companyId, String channel) {
        if (shoppingCart == null || shoppingCart.getItems() == null || shoppingCart.getItems().isEmpty()) {
            metricsService.sendBadRequestMetrics(companyId, channel, ReasonTypeEnum.ITEMS_IS_NULL, QueryController.class.getSimpleName());
            throw new BadRequestException("shoppingCart cannot be null and must have at least 1 item", "400");
        }

        if (shoppingCart.getDestination() == null || shoppingCart.getDestination().getZipcode() == null || Strings.isBlank(shoppingCart.getDestination().getZipcode())) {
            metricsService.sendBadRequestMetrics(companyId, channel, ReasonTypeEnum.ZIPCODE_IS_NULL, QueryController.class.getSimpleName());
            throw new BadRequestException("zipcode cannot be null or empty", "400");
        }

        shoppingCart.getItems().forEach(c -> {
            if ((c.getSku() == null || c.getSku().equals("")) || c.getQuantity() == null) {
                metricsService.sendBadRequestMetrics(companyId, channel, ReasonTypeEnum.QUANTITY_IS_NULL, QueryController.class.getSimpleName());
                throw new BadRequestException("sku and quantity fields cannot be null or empty in any item", "400");
            }
        });

        List<CartItem> duplicateItems = getDuplicatesMap(shoppingCart.getItems()).values().stream()
                .filter(duplicates -> duplicates.size() > 1)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        if (!duplicateItems.isEmpty()) {
            metricsService.sendBadRequestMetrics(companyId, channel, ReasonTypeEnum.DUPLICATED_ITEMS_IS_NULL, QueryController.class.getSimpleName());
            throw new BadRequestException("skus cannot be repeated, change the quantity field to more than one unit", "400");
        }
    }

    private static Map<String, List<CartItem>> getDuplicatesMap(List<CartItem> itemList) {
        List<CartItem> items = new ArrayList<>(itemList);
        return items.stream()
                .filter(c -> c.getSku() != null)
                .collect(groupingBy(CartItem::getSku));
    }

}
