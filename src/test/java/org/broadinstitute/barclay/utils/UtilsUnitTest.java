package org.broadinstitute.barclay.utils;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class UtilsUnitTest {

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNonNullThrows(){
        final Object o = null;
        Utils.nonNull(o);
    }

    @Test
    public void testNonNullDoesNotThrow(){
        final Object o = new Object();
        Assert.assertSame(Utils.nonNull(o), o);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "^The exception message$")
    public void testNonNullWithMessageThrows() {
        Utils.nonNull(null, "The exception message");
    }

    @Test
    public void testNonNullWithMessageReturn() {
        final Object testObject = new Object();
        Assert.assertSame(Utils.nonNull(testObject, "some message"), testObject);
    }


    @DataProvider
    public Object[][] testWrapParagraphData(){
        return new Object[][]{
                {"hello \nhello hello hello hello", "hello \nhello hello\nhello hello", 12},
                {"hello \nhello hello hello hello", "hello \nhello hello\nhello hello", 11},
                {"hello \nhello hello hello hello", "hello \nhello\nhello\nhello\nhello", 10},
                {"hello \n\n\n\nhello hello hello hello", "hello \n\n\n\nhello\nhello\nhello\nhello", 10},

                {"hello \nhello hello hello hello", "hello \nhello hello hello\nhello", 20},
                {"hello \nhello hello hello hello\n", "hello \nhello hello hello\nhello\n", 20},
                {"hello \nhello hello hello hello\n\n", "hello \nhello hello hello\nhello\n\n", 20},
                {"", "", 20},
                {" ", "", 20},
                {"\n", "\n", 20},

        };

    }

    @Test(dataProvider = "testWrapParagraphData")
    void testWrapParagraph(final String input, final String expectedOutput, final int width) {
        Assert.assertEquals(Utils.wrapParagraph(input, width), expectedOutput);
    }

}
