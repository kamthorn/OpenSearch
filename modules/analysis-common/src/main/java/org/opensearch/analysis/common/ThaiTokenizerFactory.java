/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.analysis.common;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.th.ThaiTokenizer;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenizerFactory;
import org.opensearch.index.analysis.Analysis;

import java.util.List;

/**
 * Factory for {@link ThaiTokenizer}
 *
 * <p>Supports an optional user dictionary so that domain-specific terms, proper nouns, jargon and
 * loanwords take precedence over the default {@link java.text.BreakIterator} segmentation
 * boundaries. The dictionary may be supplied either inline via {@code user_dictionary_rules} or
 * from a file via {@code user_dictionary}; supplying both at once is rejected.
 *
 * <p>The internal segmentation buffer can be tuned with {@code buffer_size}. Thai text is written
 * without spaces between words and frequently exceeds the default 1024 character buffer, which can
 * split a word across the buffer boundary.
 */
public class ThaiTokenizerFactory extends AbstractTokenizerFactory {

    private static final String USER_DICT_PATH_OPTION = "user_dictionary";
    private static final String USER_DICT_RULES_OPTION = "user_dictionary_rules";
    private static final String BUFFER_SIZE_OPTION = "buffer_size";

    private final CharArraySet userDictionary;
    private final int bufferSize;

    ThaiTokenizerFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, settings, name);
        this.userDictionary = getUserDictionary(env, settings);
        this.bufferSize = settings.getAsInt(BUFFER_SIZE_OPTION, ThaiTokenizer.DEFAULT_BUFFER_SIZE);
    }

    /**
     * Loads the user dictionary from either the {@code user_dictionary} file or the
     * {@code user_dictionary_rules} inline list.
     *
     * @return the user dictionary, or {@code null} when neither option is configured
     */
    static CharArraySet getUserDictionary(Environment env, Settings settings) {
        if (settings.get(USER_DICT_PATH_OPTION) != null && settings.get(USER_DICT_RULES_OPTION) != null) {
            throw new IllegalArgumentException(
                "It is not allowed to use [" + USER_DICT_PATH_OPTION + "] in conjunction" + " with [" + USER_DICT_RULES_OPTION + "]"
            );
        }
        List<String> wordList = Analysis.parseWordList(env, settings, USER_DICT_PATH_OPTION, USER_DICT_RULES_OPTION, s -> s);
        if (wordList == null || wordList.isEmpty()) {
            return null;
        }
        return new CharArraySet(wordList, false);
    }

    @Override
    public Tokenizer create() {
        return new ThaiTokenizer(userDictionary, bufferSize);
    }
}
