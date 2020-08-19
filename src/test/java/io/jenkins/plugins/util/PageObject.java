package io.jenkins.plugins.util;

import java.io.IOException;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

/**
 * Parent class for all page objects. This base class just provides access to the underlying main page and to some
 * utility methods.
 *
 * @author Ullrich Hafner
 * @see <a href="https://martinfowler.com/bliki/PageObject.html">Page Object pattern</a>
 * @deprecated UI tests should use ATH plugin tests
 */
@Deprecated
public class PageObject {
    private final HtmlPage page;

    /**
     * Creates a page object for the given HTML page.
     *
     * @param page
     *         the page that will be analyzed in tests
     */
    protected PageObject(final HtmlPage page) {
        this.page = page;
    }

    protected HtmlPage getPage() {
        return page;
    }

    /**
     * Returns the HTML page of this page object as plain text.
     *
     * @return the HTML page of this page object
     */
    public String getPageAsText() {
        return getPage().asText();
    }

    /**
     * Clicks the selected element.
     *
     * @param element
     *         the element to click
     *
     * @return the resulting HTML page
     */
    public static HtmlPage clickOnElement(final DomElement element) {
        try {
            return element.click();
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Reloads the current page.
     */
    public void refresh() {
        try {
            getPage().refresh();
        }
        catch (IOException e) {
            throw new AssertionError("WebPage refresh failed.", e);
        }
    }
}
