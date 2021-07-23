package br.com.lojasrenner.rlog.transport.order.infrastructure.service.v1.entity;

import br.com.lojasrenner.rlog.transport.order.business.helper.zipcode.ZipCodeHelper;
import br.com.lojasrenner.rlog.transport.order.infrastructure.api.v1.enums.CountryEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Optional;

@Getter
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BranchOfficeEntity {
    private String id;
    private String companyId;
    private String name;
    private String zipcode;
    private String city;
    private String state;
    private String country;
    private String latitude;
    private String longitude;
    private String branchType;
    private BranchOfficeConfigurationEntity configuration;
    private BranchOfficeStatusEntity status;

    public String getBranchOfficeId() {
        return id != null ? id.substring(3) : null;
    }

    public String getQuoteZipCode(Optional<CountryEnum> country) {
        ZipCodeHelper zipcodeHelper = null;
        if(country.isPresent())
            zipcodeHelper = country.get().getHelper();

        if(configuration != null && configuration.getQuotationZipcode() != null && zipcodeHelper != null && zipcodeHelper.isValid(configuration.getQuotationZipcode()))
            return zipcodeHelper.normalize(configuration.getQuotationZipcode());

        return zipcode;
    }
}
