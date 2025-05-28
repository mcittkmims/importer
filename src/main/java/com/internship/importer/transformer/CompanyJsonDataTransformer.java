package com.internship.importer.transformer;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.internship.importer.config.ImportAppConfig;
import com.internship.importer.helper.JsonParserHandler;
import com.internship.importer.model.CompanyDTO;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@AllArgsConstructor
public class CompanyJsonDataTransformer implements CompanyDataTransformer {

    private ImportAppConfig config;

    @Override
    public void processLargeFile(InputStream inputStream, Consumer<CompanyDTO> consumer) throws IOException {
        try (JsonParser jsonParser = new JsonFactory().createParser(inputStream)) {
            if (jsonParser.nextToken() == JsonToken.START_ARRAY) {
                while (jsonParser.nextToken() != JsonToken.END_ARRAY) {
                    CompanyDTO companyDTO = readCompanyDTO(jsonParser);
                    consumer.accept(companyDTO);
                }
            }
        }
    }

    private CompanyDTO readCompanyDTO(JsonParser jsonParser) throws IOException {
        CompanyDTO companyDTO = new CompanyDTO();
        Map<String, JsonParserHandler> fieldHandlerMap = getFieldHandlerMap(companyDTO);
        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jsonParser.currentName();
            jsonParser.nextToken();

            JsonParserHandler handler = fieldHandlerMap.get(fieldName);

            if (handler != null) {
                handler.handle(jsonParser);
            } else {
                jsonParser.skipChildren();
            }
        }
        return companyDTO;
    }


    private Map<String, JsonParserHandler> getFieldHandlerMap(CompanyDTO companyDTO) {
        Map<String, JsonParserHandler> map = new HashMap<>();

        map.put(config.getCompanyCorporateNumberField(), parser -> companyDTO.setCorporateNumber(parser.getText()));
        map.put(config.getCompanyNameField(), parser -> companyDTO.setName(parser.getText()));
        map.put(config.getCompanyStatusField(), parser -> companyDTO.setStatus(parser.getText()));
        map.put(config.getCompanyLocationField(), parser -> companyDTO.setLocation(parser.getText()));
        map.put(config.getCompanyPostalCodeField(), parser -> companyDTO.setPostalCode(parser.getText()));
        map.put(config.getCompanyRepresentativeTitleField(), parser -> companyDTO.setRepresentativeTitle(parser.getText()));
        map.put(config.getCompanyRepresentativeNameField(), parser -> companyDTO.setRepresentativeName(parser.getText()));
        map.put(config.getCompanyEmployeeCountField(), parser -> companyDTO.setEmployeeCount(Integer.parseInt(parser.getText())));
        map.put(config.getCompanyEstablishmentDateField(), parser -> companyDTO.setEstablishmentDate(LocalDate.parse(parser.getText())));
        map.put(config.getCompanyIndustriesField(), parser -> companyDTO.setIndustries(this.parseIndustries(parser)));
        return map;
    }

    
    private List<String> parseIndustries(JsonParser parser) throws IOException {
        List<String> industries = new ArrayList<>();
        if (parser.currentToken() == JsonToken.START_ARRAY) {
            while (parser.nextToken() != JsonToken.END_ARRAY) {
                industries.add(parser.getText());
            }
        } else {
            industries.add(parser.getText());
        }
        return industries;
    }
}
