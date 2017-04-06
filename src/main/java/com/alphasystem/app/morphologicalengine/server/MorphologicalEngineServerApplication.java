package com.alphasystem.app.morphologicalengine.server;

import com.alphasystem.app.morphologicalengine.docx.MorphologicalChartConfiguration;
import com.alphasystem.app.morphologicalengine.spring.MorphologicalEngineConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.HttpMessageConverters;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;

/**
 * @author sali
 */
@SpringBootApplication
@Import({MorphologicalEngineConfiguration.class, MorphologicalChartConfiguration.class})
public class MorphologicalEngineServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MorphologicalEngineServerApplication.class, args);
    }

    @Bean
    public HttpMessageConverters customConverters() {
        ByteArrayHttpMessageConverter arrayHttpMessageConverter = new ByteArrayHttpMessageConverter();
        return new HttpMessageConverters(arrayHttpMessageConverter);
    }
}
