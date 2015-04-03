/**
 *  Copyright (C) 2013 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.client

import org.junit.Assert.assertEquals
import org.junit.Test
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.{By, Keys, WebElement}
import org.orbeon.oxf.util.ScalaUtils._
import org.scalatest.junit.{AssertionsForJUnit, MustMatchersForJUnit}

import scala.util.Try

trait XForms extends AssertionsForJUnit with MustMatchersForJUnit with FormRunnerOps {

    def isInvalid(el: WebElement): Boolean = split[Set](el.getAttribute("class")).contains("xforms-invalid")
    def inErrorSummary(id: String): Boolean = Try(webDriver.findElement(By.linkText(id))).isSuccess

    // Load the page, and clears the regular input. At that point, it won't yet be marked as invalid.
    def issue619WithClearInput(block: (WebElement, WebElement) ⇒ Unit): Unit = {
        for {
            _ ← loadOrbeonPage("/unit-tests/issue-0619")
            regularSpan  ← webDriver.findElement(By.cssSelector("#regular"))
            regularInput ← regularSpan.findElement(By.cssSelector("input"))
            // Clear input
            _ ← regularInput.click()
            _ ← regularInput.clear()
            // Should not be invalid or shown in the error summary
            _ ← assert(! isInvalid(regularSpan))
            _ ← assert(! inErrorSummary("regular"))
        }
        block(regularSpan, regularInput)
    }

    @Test def issue619ErrorShownOnTabOut(): Unit =
        issue619WithClearInput { (regularSpan, regularInput) ⇒
            for {
                _ ← regularInput.sendKeys(Keys.TAB)
                _ ← assert(isInvalid(regularSpan))
                _ ← assert(inErrorSummary("regular"))
            }()
        }

    @Test def issue619ErrorShownOnClickBody(): Unit =
        issue619WithClearInput { (regularSpan, regularInput) ⇒
            for {
                _ ← webDriver.findElement(By.cssSelector("body")).click()
                _ ← assert(isInvalid(regularSpan))
                _ ← assert(inErrorSummary("regular"))
            }()
        }

    @Test def issue1749ErrorShownOnActivate(): Unit =
        issue619WithClearInput { (regularSpan, regularInput) ⇒
            for {
                _ ← regularInput.sendKeys(Keys.ENTER)
                _ ← assert(isInvalid(regularSpan))
                //_ ← assert(inErrorSummary("regular")) // FIXME
            }()
        }

    @Test def issue1750Incremental(): Unit = {
        for {
            _ ← loadOrbeonPage("/unit-tests/issue-0619")
            incrementalSpan  ← webDriver.findElement(By.cssSelector("#incremental"))
            incrementalInput ← incrementalSpan.findElement(By.cssSelector("input"))
            _ ← incrementalInput.click()
            _ ← assert(! isInvalid(incrementalSpan))
            _ ← assert(! inErrorSummary("incremental"))
            _ ← incrementalInput.sendKeys(Keys.DELETE)
            _ ← assert(isInvalid(incrementalSpan))
            //_ ← assert(inErrorSummary("incremental")) // FIXME
        }()
    }

    // https://github.com/orbeon/orbeon-forms/issues/889
    @Test def issue889(): Unit = {

        def clickCheckbox() = clickElementByCSS("#hide-checkbox input")

        def liGroupElements  = cssSelector("#group-begin-ul-group ~ li:not(.xforms-group-begin-end)").findAllElements.to[List]
        def divGroupElements = cssSelector("#div-group > div").findAllElements.to[List]

        def checkNonRelevantClasses(elements: List[Element]) = {
            elements(0).classes must not contain ("class42")
            elements(1).classes must contain ("myClass")
        }

        def checkRelevantClasses(elements: List[Element]) = {
            elements(0).classes must contain ("class42")
            elements(1).classes must contain ("myClass")
        }

        // Just after loading (checkbox is selected, content is hidden)
        loadOrbeonPage("/unit-tests/issue-0889")

        liGroupElements foreach (_.classes must contain ("xforms-disabled"))
        cssSelector("#div-group").element.classes must contain ("xforms-disabled")

        checkNonRelevantClasses(liGroupElements)
        checkNonRelevantClasses(divGroupElements)

        // Show content
        clickCheckbox()

        liGroupElements foreach (_.classes must not contain ("xforms-disabled"))
        cssSelector("#div-group").element.classes must not contain ("xforms-disabled")

        checkRelevantClasses(liGroupElements)
        checkRelevantClasses(divGroupElements)

        // Hide content again
        clickCheckbox()
        liGroupElements foreach (_.classes must contain ("xforms-disabled-subsequent"))
        cssSelector("#div-group").element.classes must contain ("xforms-disabled-subsequent")

        checkNonRelevantClasses(liGroupElements)
        checkNonRelevantClasses(divGroupElements)
    }

    // https://github.com/orbeon/orbeon-forms/commit/9bfa9ad051c2bafa8c88e8562bb55f46dd9e7666
    @Test def eventProperties(): Unit = {

        def checkOutputs(outputs: Seq[(String, String)]) =
            outputs.foreach { case (cssClass, expected) ⇒
                val actual = $("." + cssClass + " span").getText
                assertEquals(expected, actual)
            }

        loadOrbeonPage("/unit-tests/feature-event-properties")
        checkOutputs(Seq("triggered" → "false", "p1" → "", "p2" → ""))
        $("#send-event button").click()
        waitForAjaxResponse()
        checkOutputs(Seq("triggered" → "true", "p1" → "v1", "p2" → "v2"))
    }

    @Test def issue1888(): Unit = {

        val Tooltips = "ORBEON.xforms.Globals.hintTooltipForControl"
        val InputTooltip = s"$Tooltips['my-input']"

        for {
            _ ← loadOrbeonPage("/unit-tests/issue-1888")
            body   ← webDriver.findElement(By.cssSelector("body"))
            output ← webDriver.findElement(By.cssSelector("#my-output .xforms-output-output"))
            input  ← webDriver.findElement(By.cssSelector("input.xforms-input-input"))
            // At first, the tooltip isn't initialized
            _ ← assertJSExpression(s"_.isUndefined($InputTooltip)")
            // After a mouseover, we set it to null, since the message is empty
            _ ← moveToWebElement(input)
            _ ← assertJSExpression(s"_.isNull($InputTooltip)")
            // Enter "a"
            _ ← new Actions(webDriver).click().sendKeys("a").moveToElement(body).click().build().perform()
            _ ← assert(output.getText == "a")
            // On mouseover, the tooltip with "a" shows
            _ ← moveToWebElement(input)
            _ ← assertJSExpression(s"_.isObject($Tooltips['my-input'])")
            _ ← assertJSExpression(s"ORBEON.jQuery($Tooltips['my-input'].element).css('visibility') == 'visible'")
        }()
    }
}
