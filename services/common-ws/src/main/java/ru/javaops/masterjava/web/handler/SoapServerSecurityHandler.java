package ru.javaops.masterjava.web.handler;

import com.sun.xml.ws.api.handler.MessageHandlerContext;
import lombok.extern.slf4j.Slf4j;
import ru.javaops.masterjava.web.AuthUtil;

import javax.xml.ws.handler.MessageContext;
import java.util.List;
import java.util.Map;

import static ru.javaops.masterjava.web.AuthUtil.encodeBasicAuthHeader;

@Slf4j
public class SoapServerSecurityHandler extends SoapBaseHandler {
    private String authHeader;

    public SoapServerSecurityHandler(String user, String password) {
        this(encodeBasicAuthHeader(user, password));
    }

    public SoapServerSecurityHandler(String authHeader) {
        this.authHeader = authHeader;
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean handleMessage(MessageHandlerContext context) {
        if (!isOutbound(context) && authHeader != null) {
            var headers = (Map<String, List<String>>) context.get(MessageContext.HTTP_REQUEST_HEADERS);
            int code = AuthUtil.checkBasicAuth(headers, authHeader);
            if (code != 0) {
                context.put(MessageContext.HTTP_RESPONSE_CODE, code);
                throw new SecurityException();
            }
        }
        return true;
    }

    @Override
    public boolean handleFault(MessageHandlerContext context) {
        // do nothing
        return true;
    }
}
