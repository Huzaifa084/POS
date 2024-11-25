package com.devaxiom.pos.config;

import com.devaxiom.briefcase.dto.ClientProfileResponseDto;
import com.devaxiom.briefcase.dto.LawyerProfileResponseDto;
import com.devaxiom.briefcase.model.Client;
import com.devaxiom.briefcase.model.Lawyer;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;

@Slf4j
@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        // Explicit Mapping for Client -> ClientProfileResponseDto
        modelMapper.typeMap(Client.class, ClientProfileResponseDto.class).addMappings(mapper -> {
            mapper.map(Client::getEmiratesId, ClientProfileResponseDto::setEmiratesId); // Map emiratesId as String
        });

        // String to LocalDate Converter
        modelMapper.addConverter(new Converter<String, LocalDate>() {
            @Override
            public LocalDate convert(MappingContext<String, LocalDate> context) {
                log.info("Converting String to LocalDate: {}", context.getSource());
                return context.getSource() == null ? null : LocalDate.parse(context.getSource());
            }
        });
        System.out.println("Mappings: " + modelMapper.getTypeMap(Lawyer.class, LawyerProfileResponseDto.class));

        // Enable debugging
        modelMapper.getConfiguration().setAmbiguityIgnored(true);
        modelMapper.getConfiguration().setFieldMatchingEnabled(true);
        modelMapper.getConfiguration().setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        modelMapper.typeMap(Lawyer.class, LawyerProfileResponseDto.class).addMappings(mapper -> {
            mapper.skip(Lawyer::getId, LawyerProfileResponseDto::setId); // Exclude ID
            mapper.map(Lawyer::getLandlineNumber, LawyerProfileResponseDto::setLandlineNumber);
        });


        return modelMapper;
    }
}
