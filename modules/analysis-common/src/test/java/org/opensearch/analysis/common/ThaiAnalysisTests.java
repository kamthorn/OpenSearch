/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.analysis.common;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;
import org.apache.lucene.analysis.th.ThaiTokenizer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
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
        CharFilterFactory factory = analysis(
            Settings.builder().put("index.analysis.char_filter.my_thai.type", "thai_char_filter")
        ).charFilter.get("my_thai");
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
}
