package com.alphasystem.app.morphologicalengine.server.rest;

import com.alphasystem.app.morphologicalengine.conjugation.builder.ConjugationBuilder;
import com.alphasystem.app.morphologicalengine.conjugation.model.OutputFormat;
import com.alphasystem.app.morphologicalengine.docx.MorphologicalChartEngine;
import com.alphasystem.app.morphologicalengine.docx.MorphologicalChartEngineFactory;
import com.alphasystem.morphologicalanalysis.morphology.model.ConjugationData;
import com.alphasystem.morphologicalanalysis.morphology.model.ConjugationTemplate;
import com.alphasystem.morphologicalengine.model.MorphologicalChart;
import com.alphasystem.util.AppUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.alphasystem.app.morphologicalengine.conjugation.builder.ConjugationHelper.getConjugationRoots;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8_VALUE;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM_VALUE;

/**
 * @author sali
 */
@RestController
@RequestMapping("/morphologicalEngine")
public class MorphologicalEngineRestController {

    @Autowired private ConjugationBuilder conjugationBuilder;
    @Autowired private MorphologicalChartEngineFactory morphologicalChartEngineFactory;

    @CrossOrigin("*")
    @RequestMapping(value = "/morphologicalChart/format/{format}", method = RequestMethod.POST, consumes = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> morphologicalChart(@PathVariable(name = "format") OutputFormat format,
                                                     @RequestBody ConjugationTemplate conjugationTemplate) throws Exception {
        ResponseEntity<Object> result = null;
        if (OutputFormat.STREAM.equals(format)) {
            return morphologicalChart2(conjugationTemplate);
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.add("Access-Control-Allow-Methods", "POST");
            headers.add("Access-Control-Allow-Headers", "Content-Type");
            headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

            List<MorphologicalChart> morphologicalCharts = new ArrayList<>();
            final List<ConjugationData> conjugationDataList = conjugationTemplate.getData();
            conjugationDataList.forEach(conjugationData ->
                    morphologicalCharts.add(conjugationBuilder.doConjugation(getConjugationRoots(conjugationData), format)));
            result = ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_JSON_UTF8).body(morphologicalCharts);
        }
        return result;
    }

    @CrossOrigin("*")
    @RequestMapping(value = "/morphologicalChart/format/STREAM", method = RequestMethod.POST,
            produces = APPLICATION_OCTET_STREAM_VALUE, consumes = APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<Object> morphologicalChart2(@RequestBody ConjugationTemplate conjugationTemplate)
            throws Exception {
        final Path tempFile = Files.createTempFile(AppUtil.USER_TEMP_DIR.toPath(), conjugationTemplate.getId(), ".docx");
        MorphologicalChartEngine engine = morphologicalChartEngineFactory.createMorphologicalChartEngine(conjugationTemplate);
        engine.createDocument(tempFile);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Methods", "POST");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        // headers.add("Access-Control-Allow-Origin", "http://localhost:4200");
        headers.add("Content-Disposition", "filename=" + tempFile.getFileName());
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        FileSystemResource docxFile = new FileSystemResource(tempFile.toFile());
        return ResponseEntity.ok().contentLength(docxFile.contentLength()).headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(new InputStreamResource(docxFile.getInputStream()));
    }

    /*@CrossOrigin
    @RequestMapping(value = "/morphologicalChart/form/{form}/firstRadical/{firstRadical}/secondRadical/{secondRadical}/thirdRadical/{thirdRadical}",
            method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<MorphologicalChart[]> morphologicalChart(@RequestHeader(name = "format", required = false) OutputFormat format,
                                                                   @RequestHeader(name = "removePassiveLine", required = false) boolean removePassiveLine,
                                                                   @RequestHeader(name = "skipRuleProcessing", required = false) boolean skipRuleProcessing,
                                                                   @RequestHeader(name = "verbalNouns", required = false) VerbalNoun[] verbalNouns,
                                                                   @PathVariable(name = "form") NamedTemplate form,
                                                                   @RequestHeader(name = "translation", required = false) String translation,
                                                                   @PathVariable(name = "firstRadical") ArabicLetterType firstRadical,
                                                                   @PathVariable(name = "secondRadical") ArabicLetterType secondRadical,
                                                                   @PathVariable(name = "thirdRadical") ArabicLetterType thirdRadical) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
        headers.add("Access-Control-Allow-Methods", "GET");
        headers.add("Access-Control-Allow-Headers", "Content-Type");

        final NounRootBase[] verbalNounRoots = getNounRootBases(verbalNouns);
        final RootLetters rootLetters = new RootLetters(firstRadical, secondRadical, thirdRadical);
        final ConjugationRoots conjugationRoots = getConjugationRoots(form, rootLetters, translation, verbalNounRoots, null);
        conjugationRoots.getConjugationConfiguration().removePassiveLine(removePassiveLine).skipRuleProcessing(skipRuleProcessing);
        final MorphologicalChart morphologicalChart = conjugationBuilder.doConjugation(conjugationRoots, format);

        return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_JSON_UTF8).body(new MorphologicalChart[]{morphologicalChart});
    }

    private NounRootBase[] getNounRootBases(VerbalNoun[] verbalNouns) {
        NounRootBase[] verbalNounRoots = null;
        if (!ArrayUtils.isEmpty(verbalNouns)) {
            verbalNounRoots = new NounRootBase[verbalNouns.length];
            for (int i = 0; i < verbalNouns.length; i++) {
                verbalNounRoots[i] = VerbalNounFactory.getByVerbalNoun(verbalNouns[i]);
            }
        }
        return verbalNounRoots;
    }

    @CrossOrigin
    @RequestMapping(value = "/morphologicalChart2/form/{form}/firstRadical/{firstRadical}/secondRadical/{secondRadical}/thirdRadical/{thirdRadical}",
            method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<InputStreamResource> morphologicalChart(@PathVariable(name = "form") NamedTemplate form,
                                                                  @PathVariable(name = "firstRadical") ArabicLetterType firstRadical,
                                                                  @PathVariable(name = "secondRadical") ArabicLetterType secondRadical,
                                                                  @PathVariable(name = "thirdRadical") ArabicLetterType thirdRadical,
                                                                  @RequestHeader(name = "translation", required = false) String translation,
                                                                  @RequestHeader(name = "removePassiveLine", required = false) boolean removePassiveLine,
                                                                  @RequestHeader(name = "skipRuleProcessing", required = false) boolean skipRuleProcessing,
                                                                  @RequestHeader(name = "verbalNouns", required = false) VerbalNoun[] verbalNouns) throws Exception {
        final RootLetters rootLetters = new RootLetters(firstRadical, secondRadical, thirdRadical);
        ConjugationData conjugationData = new ConjugationData();
        conjugationData.setRootLetters(rootLetters);
        conjugationData.setTemplate(form);
        conjugationData.setTranslation(translation);
        if (!ArrayUtils.isEmpty(verbalNouns)) {
            conjugationData.setVerbalNouns(Arrays.asList(verbalNouns));
        }
        conjugationData.getConfiguration().skipRuleProcessing(skipRuleProcessing).removePassiveLine(removePassiveLine);
        ConjugationTemplate conjugationTemplate = new ConjugationTemplate();
        conjugationTemplate.getData().add(conjugationData);

        final Path tempFile = Files.createTempFile(AppUtil.USER_TEMP_DIR.toPath(), "", ".docx");
        MorphologicalChartEngine engine = morphologicalChartEngineFactory.createMorphologicalChartEngine(conjugationTemplate);
        engine.createDocument(tempFile);

        FileSystemResource docxFile = new FileSystemResource(tempFile.toFile());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.add("Access-Control-Allow-Methods", "GET");
        headers.add("Access-Control-Allow-Headers", "Content-Type");
        headers.add("Content-Disposition", "filename=" + tempFile.getFileName());
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        return ResponseEntity.ok().contentLength(docxFile.contentLength()).headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(docxFile.getInputStream()));
    }*/

}
