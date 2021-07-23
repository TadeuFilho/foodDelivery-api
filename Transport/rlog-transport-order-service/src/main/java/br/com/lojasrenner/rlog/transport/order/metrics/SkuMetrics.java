package br.com.lojasrenner.rlog.transport.order.metrics;

import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

@Component
public class SkuMetrics extends BaseMetrics {

    @Override
    protected String getMetricName() {
        return "skuMetrics";
    }

    public void sendMetricsForSku(String companyId, String channel, String sku, String type){
        Counter.Builder builder = beginCounter("bfl_delivery_options_sku_problems", "count for skus with problems partial and unavailable");
        addTag(companyId, channel, builder, "companyId", companyId);
        addTag(companyId, channel, builder, "channel", channel);
        addTag(companyId, channel, builder, "sku", sku);
        addTag(companyId, channel, builder, "type", type);

        Counter counter = register(companyId, channel, builder);
        increment(counter, 1);
    }
}
