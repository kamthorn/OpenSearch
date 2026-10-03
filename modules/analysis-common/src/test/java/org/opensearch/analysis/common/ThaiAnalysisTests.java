/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.analysis.common;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.th.ThaiAnalyzer;
import org.apache.lucene.analysis.th.ThaiTokenizer;
import org.apache.lucene.util.BytesRef;
import org.opensearch.Version;
import org.opensearch.cluster.metadata.IndexMetadata;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.analysis.Analysis;
import org.opensearch.index.analysis.AnalysisTestsHelper;
import org.opensearch.index.analysis.CharFilterFactory;
import org.opensearch.index.analysis.TokenFilterFactory;
import org.opensearch.index.analysis.TokenizerFactory;
import org.opensearch.test.OpenSearchTestCase;
import org.opensearch.test.OpenSearchTokenStreamTestCase;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

public class ThaiAnalysisTests extends OpenSearchTokenStreamTestCase {

    private static OpenSearchTestCase.TestAnalysis analysis(Settings.Builder builder) throws IOException {
        return AnalysisTestsHelper.createTestAnalysisFromSettings(
            builder.put(Environment.PATH_HOME_SETTING.getKey(), createTempDir().toString()).build(),
            new CommonAnalysisModulePlugin()
        );
    }

    public void testTokenizerWithoutUserDictionary() throws IOException {
        TokenizerFactory factory = analysis(Settings.builder().put("index.analysis.tokenizer.my_thai.type", "thai")).tokenizer.get(
            "my_thai"
        );
        Tokenizer tokenizer = factory.create();
        tokenizer.setReader(new StringReader("ภาษาไทย"));
        assertTokenStreamContents(tokenizer, new String[] { "ภาษา", "ไทย" });
    }

    public void testTokenizerUserDictionaryRules() throws IOException {
        TokenizerFactory factory = analysis(
            Settings.builder()
                .put("index.analysis.tokenizer.my_thai.type", "thai")
                .putList("index.analysis.tokenizer.my_thai.user_dictionary_rules", "พารากอน", "คนขับรถ")
        ).tokenizer.get("my_thai");
        Tokenizer tokenizer = factory.create();
        tokenizer.setReader(new StringReader("เขาเป็นคนขับรถไปพารากอน"));
        assertTokenStreamContents(
            tokenizer,
            new String[] { "เขา", "เป็น", "คนขับรถ", "ไป", "พารากอน" },
            new int[] { 0, 3, 7, 14, 16 },
            new int[] { 3, 7, 14, 16, 23 }
        );
    }

    public void testTokenizerUserDictionaryFile() throws IOException {
        Path home = createTempDir();
        Path config = home.resolve("config");
        Files.createDirectories(config);
        Files.write(config.resolve("thai_dict.txt"), List.of("# comment", "พารากอน"));
        TokenizerFactory factory = AnalysisTestsHelper.createTestAnalysisFromSettings(
            Settings.builder()
                .put(Environment.PATH_HOME_SETTING.getKey(), home.toString())
                .put("index.analysis.tokenizer.my_thai.type", "thai")
                .put("index.analysis.tokenizer.my_thai.user_dictionary", "thai_dict.txt")
                .build(),
            new CommonAnalysisModulePlugin()
        ).tokenizer.get("my_thai");
        Tokenizer tokenizer = factory.create();
        tokenizer.setReader(new StringReader("ไปพารากอนกัน"));
        assertTokenStreamContents(tokenizer, new String[] { "ไป", "พารากอน", "กัน" });
    }

    public void testTokenizerUserDictionaryAndRulesAreMutuallyExclusive() {
        IllegalArgumentException e = expectThrows(
            IllegalArgumentException.class,
            () -> analysis(
                Settings.builder()
                    .put("index.analysis.tokenizer.my_thai.type", "thai")
                    .put("index.analysis.tokenizer.my_thai.user_dictionary", "thai_dict.txt")
                    .putList("index.analysis.tokenizer.my_thai.user_dictionary_rules", "พารากอน")
            )
        );
        assertThat(
            e.getMessage(),
            containsString("It is not allowed to use [user_dictionary] in conjunction with [user_dictionary_rules]")
        );
    }

    public void testTokenizerBufferSize() throws IOException {
        TokenizerFactory factory = analysis(
            Settings.builder().put("index.analysis.tokenizer.my_thai.type", "thai").put("index.analysis.tokenizer.my_thai.buffer_size", 32)
        ).tokenizer.get("my_thai");
        Tokenizer tokenizer = factory.create();
        tokenizer.setReader(new StringReader("การทดสอบ ภาษาไทย มหาวิทยาลัย"));
        assertTokenStreamContents(tokenizer, new String[] { "การ", "ทดสอบ", "ภาษา", "ไทย", "มหาวิทยาลัย" });
    }

    public void testCharFilter() throws IOException {
        CharFilterFactory factory = analysis(Settings.builder().put("index.analysis.char_filter.my_thai.type", "thai")).charFilter.get(
            "my_thai"
        );
        Reader reader = factory.create(new StringReader("เเมว"));
        Tokenizer tokenizer = new ThaiTokenizer();
        tokenizer.setReader(reader);
        assertTokenStreamContents(tokenizer, new String[] { "แมว" }, new int[] { 0 }, new int[] { 4 }, 4);
    }

    public void testRepeatFilter() throws IOException {
        TokenFilterFactory factory = analysis(Settings.builder().put("index.analysis.filter.my_repeat.type", "thai_repeat")).tokenFilter
            .get("my_repeat");
        Tokenizer tokenizer = new ThaiTokenizer();
        tokenizer.setReader(new StringReader("วิ่งเร็วๆ"));
        assertTokenStreamContents(
            factory.create(tokenizer),
            new String[] { "วิ่ง", "เร็ว", "เร็ว" },
            new int[] { 0, 4, 8 },
            new int[] { 4, 8, 9 }
        );
    }

    public void testNormalizationFilter() throws IOException {
        TokenFilterFactory factory = analysis(
            Settings.builder().put("index.analysis.filter.my_norm.type", "thai_normalization")
        ).tokenFilter.get("my_norm");
        Tokenizer tokenizer = new KeywordTokenizer();
        tokenizer.setReader(new StringReader("บ่ัน"));
        assertTokenStreamContents(factory.create(tokenizer), new String[] { "บั่น" });
    }

    private static final String LEGACY_TEXT = "ผลไม้และการเปิดตัวสินค้า";

    public void testAnalyzer() throws IOException {
        Analyzer analyzer = analysis(Settings.builder().put("index.analysis.analyzer.my_thai.type", "thai")).indexAnalyzers.get("my_thai")
            .analyzer();
        assertThat(analyzer, instanceOf(ThaiAnalyzer.class));
        assertAnalyzesTo(analyzer, "เเมววิ่งเร็วๆ", new String[] { "แมว", "วิ่ง", "เร็ว", "เร็ว" });
        // Lucene's curated default stopwords keep content words such as เปิด
        assertAnalyzesTo(analyzer, LEGACY_TEXT, new String[] { "ผลไม้", "เปิด", "ตัว", "สินค้า" });
        assertEquals(new BytesRef("แมว"), analyzer.normalize("field", "เเมว"));
    }

    public void testPreBuiltAnalyzer() throws IOException {
        Analyzer analyzer = analysis(Settings.builder()).indexAnalyzers.get("thai").analyzer();
        assertThat(analyzer, instanceOf(ThaiAnalyzer.class));
        assertAnalyzesTo(analyzer, "เเมววิ่งเร็วๆ", new String[] { "แมว", "วิ่ง", "เร็ว", "เร็ว" });
    }

    public void testAnalyzerStopwords() throws IOException {
        Analyzer analyzer = analysis(
            Settings.builder()
                .put("index.analysis.analyzer.my_thai.type", "thai")
                .putList("index.analysis.analyzer.my_thai.stopwords", "แมว")
        ).indexAnalyzers.get("my_thai").analyzer();
        assertAnalyzesTo(analyzer, "เเมววิ่ง", new String[] { "วิ่ง" });
    }

    /** Indices created before Lucene 10.6 must produce the same terms as before, so that they do not need a reindex. */
    public void testAnalyzerOnOlderIndex() throws IOException {
        OpenSearchTestCase.TestAnalysis analysis = analysis(
            Settings.builder()
                .put(IndexMetadata.SETTING_VERSION_CREATED, Version.V_3_9_0)
                .put("index.analysis.analyzer.my_thai.type", "thai")
        );
        for (Analyzer analyzer : List.of(
            analysis.indexAnalyzers.get("my_thai").analyzer(),
            analysis.indexAnalyzers.get("thai").analyzer()
        )) {
            assertThat(analyzer, instanceOf(LegacyThaiAnalyzer.class));
            // no char filter, normalization or repeat expansion
            assertAnalyzesTo(analyzer, "เเมวนอนหลับ", new String[] { "เเมวน", "อ", "น", "หลับ" });
            assertAnalyzesTo(analyzer, "สวัสดีๆ", new String[] { "สวัสดี", "ๆ" });
            // the previous default stopword list removes เปิด
            assertAnalyzesTo(analyzer, LEGACY_TEXT, new String[] { "ผลไม้", "ตัว", "สินค้า" });
        }
    }

    public void testNamedStopWordsAreFrozen() {
        // _thai_ keeps resolving to the list shipped with Lucene up to 10.5, whatever the Lucene version is
        CharArraySet thai = (CharArraySet) Analysis.NAMED_STOP_WORDS.get("_thai_");
        assertThat(thai.size(), equalTo(115));
        assertTrue(thai.contains("เปิด"));
        assertTrue(thai.contains("ผล"));
        assertFalse(thai.contains("สำหรับ"));
    }

    public void testLegacyTokenizerSafeEnd() {
        LegacyThaiTokenizer tokenizer = new LegacyThaiTokenizer();
        assertTrue(tokenizer.isSafeEnd('\n'));
        assertTrue(tokenizer.isSafeEnd('\u2029'));
        assertFalse(tokenizer.isSafeEnd(' '));
    }

    public void testTokenizerOnOlderIndex() throws IOException {
        TokenizerFactory factory = analysis(
            Settings.builder()
                .put(IndexMetadata.SETTING_VERSION_CREATED, Version.V_3_9_0)
                .put("index.analysis.tokenizer.my_thai.type", "thai")
        ).tokenizer.get("my_thai");
        assertThat(factory.create(), instanceOf(LegacyThaiTokenizer.class));
    }
}
