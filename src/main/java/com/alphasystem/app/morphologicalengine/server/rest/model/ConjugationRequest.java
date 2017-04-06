package com.alphasystem.app.morphologicalengine.server.rest.model;

import com.alphasystem.morphologicalanalysis.morphology.model.ChartConfiguration;
import com.alphasystem.morphologicalanalysis.morphology.model.ConjugationData;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @author sali
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConjugationRequest {

    private ChartConfiguration chartConfiguration;
    private ConjugationData conjugationData;

    public ChartConfiguration getChartConfiguration() {
        return chartConfiguration;
    }

    public void setChartConfiguration(ChartConfiguration chartConfiguration) {
        this.chartConfiguration = chartConfiguration;
    }

    public ConjugationData getConjugationData() {
        return conjugationData;
    }

    public void setConjugationData(ConjugationData conjugationData) {
        this.conjugationData = conjugationData;
    }
}
