package de.lpchat;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class QuietLoggingTest {
    @Test void pluginLoggerEmitsNothingWithoutChangingServerLogger() {
        Logger server=Logger.getAnonymousLogger(); server.setLevel(Level.INFO);
        Logger plugin=Logger.getAnonymousLogger(); plugin.setParent(server);
        AtomicInteger output=new AtomicInteger();
        plugin.addHandler(new Handler() {
            public void publish(LogRecord record) { output.incrementAndGet(); }
            public void flush() {} public void close() {}
        });
        LPChatPlugin.silenceLogger(plugin);
        for(Level level : new Level[]{Level.FINE,Level.INFO,Level.WARNING,Level.SEVERE}) plugin.log(level,"test");
        assertEquals(0,output.get());
        assertEquals(Level.INFO,server.getLevel());
        assertFalse(plugin.getUseParentHandlers());
    }
}
