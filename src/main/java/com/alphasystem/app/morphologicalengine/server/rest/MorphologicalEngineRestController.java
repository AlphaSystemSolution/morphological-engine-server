package com.alphasystem.app.morphologicalengine.server.rest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.alphasystem.app.morphologicalengine.conjugation.builder.AbbreviatedConjugationBuilder;
import com.alphasystem.app.morphologicalengine.conjugation.builder.ConjugationBuilder;
import com.alphasystem.app.morphologicalengine.conjugation.builder.ConjugationHelper;
import com.alphasystem.app.morphologicalengine.conjugation.builder.ConjugationRoots;
import com.alphasystem.app.morphologicalengine.conjugation.builder.DetailedConjugationBuilder;
import com.alphasystem.app.morphologicalengine.conjugation.model.Form;
import com.alphasystem.app.morphologicalengine.conjugation.model.NounRootBase;
import com.alphasystem.app.morphologicalengine.conjugation.model.OutputFormat;
import com.alphasystem.app.morphologicalengine.conjugation.model.VerbRootBase;
import com.alphasystem.app.morphologicalengine.conjugation.model.VerbalNounFactory;
import com.alphasystem.app.morphologicalengine.docx.MorphologicalChartEngine;
import com.alphasystem.app.morphologicalengine.docx.MorphologicalChartEngineFactory;
import com.alphasystem.arabic.model.ArabicLetterType;
import com.alphasystem.arabic.model.NamedTemplate;
import com.alphasystem.morphologicalanalysis.morphology.model.ConjugationData;
import com.alphasystem.morphologicalanalysis.morphology.model.ConjugationTemplate;
import com.alphasystem.morphologicalanalysis.morphology.model.RootLetters;
import com.alphasystem.morphologicalanalysis.morphology.model.support.SarfTermType;
import com.alphasystem.morphologicalanalysis.morphology.model.support.VerbalNoun;
import com.alphasystem.morphologicalengine.model.AbbreviatedConjugation;
import com.alphasystem.morphologicalengine.model.ConjugationGroup;
import com.alphasystem.morphologicalengine.model.MorphologicalChart;
import com.alphasystem.util.AppUtil;

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
	@Autowired private AbbreviatedConjugationBuilder abbreviatedConjugationBuilder;
	@Autowired private DetailedConjugationBuilder detailedConjugationBuilder;
	@Autowired private MorphologicalChartEngineFactory morphologicalChartEngineFactory;

	@CrossOrigin("*")
	@RequestMapping(value = "/morphologicalChart/format/{format}", method = RequestMethod.POST, consumes = APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<Object> morphologicalChart(@PathVariable(name = "format") OutputFormat format,
													 @RequestBody ConjugationTemplate conjugationTemplate) throws Exception {
		ResponseEntity<Object> result;
		if (OutputFormat.STREAM.equals(format)) {
			return morphologicalChart2(conjugationTemplate);
		} else {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Access-Control-Allow-Methods", "POST");
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

	@CrossOrigin("*")
	@RequestMapping(value = "/AbbreviatedConjugation/template/{template}/format/{format}", method = RequestMethod.GET,
			consumes = APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<AbbreviatedConjugation> doAbbreviatedConjugation(
			@PathVariable(name = "template") NamedTemplate template,
			@PathVariable(name = "format") OutputFormat format,
			@RequestHeader(name = "firstRadical") ArabicLetterType firstRadical,
			@RequestHeader(name = "secondRadical") ArabicLetterType secondRadical,
			@RequestHeader(name = "thirdRadical") ArabicLetterType thirdRadical,
			@RequestHeader(name = "fourthRadical", required = false) ArabicLetterType fourthRadical,
			@RequestHeader(name = "verbalNouns", required = false) VerbalNoun[] verbalNouns,
			@RequestHeader(name = "translation", required = false) String translation,
			@RequestHeader(name = "skipRuleProcessing", required = false) boolean skipRuleProcessing,
			@RequestHeader(name = "removePassiveLine", required = false) boolean removePassiveLine) {
		RootLetters rootLetters = new RootLetters(firstRadical, secondRadical, thirdRadical, fourthRadical);

		NounRootBase[] verbalNounRootBases = null;
		if (!ArrayUtils.isEmpty(verbalNouns)) {
			verbalNounRootBases = VerbalNounFactory.getByVerbalNouns(Arrays.asList(verbalNouns));
		}
		final ConjugationRoots conjugationRoots = ConjugationHelper.getConjugationRoots(template, rootLetters, translation,
				verbalNounRootBases, null);
		final AbbreviatedConjugation abbreviatedConjugation = abbreviatedConjugationBuilder.doAbbreviatedConjugation(conjugationRoots, format);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Access-Control-Allow-Methods", "GET");
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_JSON_UTF8).body(abbreviatedConjugation);
	}

	@CrossOrigin("*")
	@RequestMapping(value = "/AbbreviatedConjugation/format/{format}", method = RequestMethod.POST,
			consumes = APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<AbbreviatedConjugation[]> doAbbreviatedConjugations(
			@PathVariable(name = "format") OutputFormat format,
			@RequestBody ConjugationTemplate conjugationTemplate) {

		HttpHeaders headers = new HttpHeaders();
		headers.add("Access-Control-Allow-Methods", "POST");
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

		List<AbbreviatedConjugation> abbreviatedConjugations = new ArrayList<>();
		final List<ConjugationData> conjugationDataList = conjugationTemplate.getData();

		conjugationDataList.forEach(conjugationData ->
				abbreviatedConjugations.add(abbreviatedConjugationBuilder.doAbbreviatedConjugation(getConjugationRoots(conjugationData), format)));
		final AbbreviatedConjugation[] conjugations = abbreviatedConjugations.toArray(new AbbreviatedConjugation[abbreviatedConjugations.size()]);
		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_JSON_UTF8).body(conjugations);
	}

	@CrossOrigin("*")
	@RequestMapping(value = "/DetailedConjugation/type/{type}/template/{template}/format/{format}", method = RequestMethod.GET,
			consumes = APPLICATION_JSON_UTF8_VALUE)
	public ResponseEntity<ConjugationGroup[]> doDetailedConjugation(
			@PathVariable(name = "type") SarfTermType sarfTermType,
			@PathVariable(name = "template") NamedTemplate template,
			@PathVariable(name = "format") OutputFormat format,
			@RequestHeader(name = "firstRadical") ArabicLetterType firstRadical,
			@RequestHeader(name = "secondRadical") ArabicLetterType secondRadical,
			@RequestHeader(name = "thirdRadical") ArabicLetterType thirdRadical,
			@RequestHeader(name = "fourthRadical", required = false) ArabicLetterType fourthRadical,
			@RequestHeader(name = "verbalNouns", required = false) VerbalNoun[] verbalNouns,
			@RequestHeader(name = "skipRuleProcessing", required = false) boolean skipRuleProcessing) {

		RootLetters rootLetters = new RootLetters(firstRadical, secondRadical, thirdRadical, fourthRadical);
		final String templateId = String.format("%s_%s_%s", sarfTermType.name(), template.name(), rootLetters.getName());
		ConjugationGroup[] groups = doConjugate(sarfTermType, template, format, skipRuleProcessing, rootLetters, verbalNouns);

		HttpHeaders headers = new HttpHeaders();
		headers.add("Access-Control-Allow-Methods", "GET");
		headers.set("templateId", templateId);
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);

		return ResponseEntity.ok().headers(headers).contentType(MediaType.APPLICATION_JSON_UTF8).body(groups);
	}

	private ConjugationGroup[] doConjugate(SarfTermType sarfTermType, NamedTemplate template, OutputFormat format,
										   boolean skipRuleProcessing, RootLetters rootLetters, VerbalNoun[] verbalNouns) {
		ConjugationGroup[] groups = null;
		final Form form = Form.getByTemplate(template);
		switch (sarfTermType) {
			case PAST_TENSE:
			case PRESENT_TENSE:
			case PAST_PASSIVE_TENSE:
			case PRESENT_PASSIVE_TENSE:
			case IMPERATIVE:
			case FORBIDDING:
				final VerbRootBase[] verbRootBases = ConjugationHelper.getVerbRootBases(sarfTermType, form);
				groups = detailedConjugationBuilder.doConjugate(sarfTermType, template, rootLetters, verbRootBases,
						skipRuleProcessing, format);
				break;
			case ACTIVE_PARTICIPLE_MASCULINE:
			case ACTIVE_PARTICIPLE_FEMININE:
			case PASSIVE_PARTICIPLE_MASCULINE:
			case PASSIVE_PARTICIPLE_FEMININE:
			case VERBAL_NOUN:
			case NOUN_OF_PLACE_AND_TIME:
				NounRootBase[] nounRootBases = ConjugationHelper.getNounRootBase(sarfTermType, form, verbalNouns);
				groups = detailedConjugationBuilder.doConjugate(sarfTermType, template, rootLetters, nounRootBases,
						skipRuleProcessing, format);
				break;
		}
		return groups;
	}

}
