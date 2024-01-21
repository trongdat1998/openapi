package io.bhex.openapi.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

@Slf4j
@RestController
public class OpenApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public String error404() throws IOException {
        Writer writer = new StringWriter();

        writer.append("<html><body><h2>404 Not found</h2></body></html>");
        return writer.toString();
    }

    @Override
    public String getErrorPath() {
        return "/error";
    }

}
