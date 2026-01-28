/*
 * Briar Desktop
 * Copyright (C) 2025 The Briar Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.anonchatsecure.anonchat.test;

import static org.anonchatsecure.bramble.api.identity.AuthorConstants.MAX_AUTHOR_NAME_LENGTH;
import static org.anonchatsecure.bramble.util.StringUtils.getRandomString;
import static org.anonchatsecure.anonchat.api.forum.ForumConstants.MAX_FORUM_NAME_LENGTH;

public interface TestData {

	String AUTHOR_NAMES[] = {
			"Thales",
			"Pythagoras",
			"Plato",
			"Aristotle",
			"Euclid",
			"Archimedes",
			"Hipparchus",
			"Ptolemy",
			"Sun Tzu",
			"Ibrahim ibn Sinan",
			"Muhammad Al-Karaji",
			"Yang Hui",
			"Ren\u00e9 Descartes",
			"Pierre de Fermat",
			"Blaise Pascal",
			"Jacob Bernoulli",
			"Christian Goldbach",
			"Leonhard Euler",
			"Joseph Louis Lagrange",
			"Pierre-Simon Laplace",
			"Joseph Fourier",
			"Carl Friedrich Gauss",
			"Charles Babbage",
			"George Boole",
			"John Venn",
			"Gottlob Frege",
			"Henri Poincar\u00e9",
			"David Hilbert",
			"Bertrand Russell",
			"John von Neumann",
			"Kurt G\u00f6del",
			"Alan Turing",
			"Beno\u00eet Mandelbrot",
			"John Nash",
			getRandomString(MAX_AUTHOR_NAME_LENGTH),
			getRandomString(MAX_AUTHOR_NAME_LENGTH),
			getRandomString(MAX_AUTHOR_NAME_LENGTH),
			getRandomString(MAX_AUTHOR_NAME_LENGTH),
			getRandomString(MAX_AUTHOR_NAME_LENGTH),
			getRandomString(MAX_AUTHOR_NAME_LENGTH),
			getRandomString(MAX_AUTHOR_NAME_LENGTH),
	};

	String GROUP_NAMES[] = {
			"Private Messengers",
			"The Darknet",
			"Bletchley Park",
			"Acropolis",
			"General Discussion",
			"The Undiscovered Country",
			"The Place to Be",
			"Forum Romanum",
			getRandomString(MAX_FORUM_NAME_LENGTH),
			getRandomString(MAX_FORUM_NAME_LENGTH),
			getRandomString(MAX_FORUM_NAME_LENGTH),
			getRandomString(MAX_FORUM_NAME_LENGTH),
	};

}
