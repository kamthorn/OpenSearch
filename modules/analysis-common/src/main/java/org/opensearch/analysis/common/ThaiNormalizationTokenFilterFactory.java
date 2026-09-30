/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.analysis.common;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.th.ThaiNormalizationFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

/**
 * Factory for {@link ThaiNormalizationFilter}.
 *
 * <p>Applies Thai-specific token normalization, which must match between index and search time.
 * The filter also participates in {@link #normalize(TokenStream)} so that the analyzer's
 * normalization chain stays consistent.
 */
public class ThaiNormalizationTokenFilterFactory extends AbstractTokenFilterFactory {

    ThaiNormalizationTokenFilterFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new ThaiNormalizationFilter(tokenStream);
    }

    @Override
    public TokenStream normalize(TokenStream tokenStream) {
        return create(tokenStream);
    }
}
