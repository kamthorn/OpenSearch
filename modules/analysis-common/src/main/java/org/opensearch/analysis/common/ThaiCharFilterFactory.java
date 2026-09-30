/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.analysis.common;

import org.apache.lucene.analysis.th.ThaiCharFilter;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractCharFilterFactory;

import java.io.Reader;

/**
 * Factory for {@link ThaiCharFilter}.
 *
 * <p>Normalizes Thai text before tokenization: it removes the deprecated characters and collapses
 * the various representations of the same letter (for example {@code ำ} written as {@code ํ} +
 * {@code า}) so that otherwise identical words are indexed and matched consistently.
 */
public class ThaiCharFilterFactory extends AbstractCharFilterFactory {

    ThaiCharFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        super(indexSettings, name);
    }

    @Override
    public Reader create(Reader reader) {
        return new ThaiCharFilter(reader);
    }
}
