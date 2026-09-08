package com.alok.home.service;

import com.alok.home.commons.entity.*;
import com.alok.home.commons.repository.*;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;

@Slf4j
public abstract class GoogleSheetService {

    public abstract void refreshSheet() throws IOException;

    public Flux<String> refreshSheetStream() throws IOException {
        log.warn("Not implemented");
        return Flux.empty();
    }

}