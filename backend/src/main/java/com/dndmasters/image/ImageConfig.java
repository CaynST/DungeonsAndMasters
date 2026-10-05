package com.dndmasters.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImageConfig {

    @Bean
    public ImageProvider imageProvider(@Value("${app.image.provider:placeholder}") String provider) {
        // Only the placeholder provider exists for now; a real one can be selected by id.
        return new PlaceholderImageProvider();
    }
}
