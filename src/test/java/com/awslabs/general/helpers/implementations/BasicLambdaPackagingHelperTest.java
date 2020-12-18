package com.awslabs.general.helpers.implementations;

import com.awslabs.general.helpers.interfaces.ProcessHelper;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BasicLambdaPackagingHelperTest {
    private BasicLambdaPackagingHelper basicLambdaPackagingHelper;
    private Process process;

    @Before
    public void setup() throws IOException {
        basicLambdaPackagingHelper = spy(new BasicLambdaPackagingHelper());
        ProcessHelper processHelper = spy(new BasicProcessHelper());
        basicLambdaPackagingHelper.processHelper = processHelper;
        ProcessBuilder processBuilder = spy(new ProcessBuilder());
        process = mock(Process.class);
        when(process.getErrorStream()).thenReturn(new ByteArrayInputStream("junk".getBytes()));

        doReturn(processBuilder).when(processHelper).getProcessBuilder(any());
        doReturn(process).when(processBuilder).start();
    }

    private void setVersionStringResult(String versionStringResult) {
        when(process.getInputStream()).thenReturn(new ByteArrayInputStream(versionStringResult.getBytes()));
    }

    @Test
    public void shouldReturnTrueWithVersion19X() {
        setVersionStringResult("pip 19.1.2");

        assertThat(basicLambdaPackagingHelper.isCorrectPipVersion(), is(true));
    }

    @Test
    public void shouldReturnFalseWithVersion20X() {
        setVersionStringResult("pip 20.1.2");

        assertThat(basicLambdaPackagingHelper.isCorrectPipVersion(), is(false));
    }
}
