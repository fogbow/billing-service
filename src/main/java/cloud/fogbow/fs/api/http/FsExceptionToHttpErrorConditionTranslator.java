package cloud.fogbow.fs.api.http;

import org.springframework.web.bind.annotation.ControllerAdvice;
import cloud.fogbow.common.http.FogbowExceptionToHttpErrorConditionTranslator;

@ControllerAdvice
public class FsExceptionToHttpErrorConditionTranslator extends FogbowExceptionToHttpErrorConditionTranslator {
}
