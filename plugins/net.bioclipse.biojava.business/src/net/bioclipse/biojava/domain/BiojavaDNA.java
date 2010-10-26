/* *****************************************************************************
 * Copyright (c) 2007-2008 The Bioclipse Project and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * www.eclipse.org—epl-v10.html <http://www.eclipse.org/legal/epl-v10.html>
 *
 * Contributors:
 *     Jonathan Alvarsson
 *     Ola Spjuth
 *
 ******************************************************************************/
package net.bioclipse.biojava.domain;

import org.biojava.bio.seq.Sequence;

import net.bioclipse.core.api.domain.IDNA;

public class BiojavaDNA extends BiojavaSequence
                        implements IDNA {

    public BiojavaDNA(Sequence seq) {
        super(seq);
    }

    public BiojavaDNA() {
        super();
    }

    public String toString() {
        return "DNA " + sequence.getName() + ": '"
               + sequence.seqString() + "'";
    }
}
