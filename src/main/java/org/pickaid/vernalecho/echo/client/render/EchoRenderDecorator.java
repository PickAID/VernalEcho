package org.pickaid.vernalecho.echo.client.render;

@FunctionalInterface
public interface EchoRenderDecorator {
    void decorate(EchoPoseRenderContext context);
}
