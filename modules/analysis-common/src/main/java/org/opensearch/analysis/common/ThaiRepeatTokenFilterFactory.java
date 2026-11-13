/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.analysis.common;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.th.ThaiRepeatFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

/**
 * Factory for {@link ThaiRepeatFilter}.
 *
 * <p>Expands the Thai repetition mark Maiyamok ({@code ๆ}) into an explicit repetition of the
 * preceding word, so that {@code "เร็วๆ"} and {@code "เร็ว เร็ว"} produce the same terms and
 * phrase queries behave correctly.
 */
public class ThaiRepeatTokenFilterFactory extends AbstractTokenFilterFactory {

    ThaiRepeatTokenFilterFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings) {
        super(indexSettings, name, settings);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new ThaiRepeatFilter(tokenStream);
    }
}
