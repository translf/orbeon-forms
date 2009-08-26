/**
 * Copyright (C) 2009 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms.processor.handlers;

import org.orbeon.oxf.xforms.XFormsUtils;
import org.orbeon.oxf.xforms.control.XFormsSingleNodeControl;
import org.orbeon.oxf.xforms.control.controls.XFormsOutputControl;
import org.orbeon.oxf.xml.XMLConstants;
import org.orbeon.oxf.xml.XMLUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Handler for xforms:output[@mediatype = 'text/html'].
 */
public class XFormsOutputHTMLHandler extends XFormsOutputHandler {

    private static final String ENCLOSING_ELEMENT_NAME = "div";

    @Override
    protected void handleControlStart(String uri, String localname, String qName, Attributes attributes, String staticId, String effectiveId, XFormsSingleNodeControl xformsControl) throws SAXException {

        final XFormsOutputControl outputControl = (XFormsOutputControl) xformsControl;
        final boolean isConcreteControl = outputControl != null;
        final ContentHandler contentHandler = handlerContext.getController().getOutput();
        final String xhtmlPrefix = handlerContext.findXHTMLPrefix();

        final AttributesImpl containerAttributes = getContainerAttributes(uri, localname, attributes, effectiveId, outputControl);

        // Handle accessibility attributes on <div>
        handleAccessibilityAttributes(attributes, containerAttributes);

        final String enclosingElementQName = XMLUtils.buildQName(xhtmlPrefix, ENCLOSING_ELEMENT_NAME);
        contentHandler.startElement(XMLConstants.XHTML_NAMESPACE_URI, ENCLOSING_ELEMENT_NAME, enclosingElementQName, containerAttributes);
        {
            if (isConcreteControl) {
                final String mediatypeValue = attributes.getValue("mediatype");
                final String htmlValue = XFormsOutputControl.getExternalValue(pipelineContext, outputControl, mediatypeValue);
                XFormsUtils.streamHTMLFragment(contentHandler, htmlValue, outputControl.getLocationData(), xhtmlPrefix);
            }
        }
        contentHandler.endElement(XMLConstants.XHTML_NAMESPACE_URI, ENCLOSING_ELEMENT_NAME, enclosingElementQName);
    }
}
