
import org.example.ApplicationStatusResponse;
import org.example.Client;
import org.example.Handler;
import org.example.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HandlerTest {
    @Mock
    Client client;
    @InjectMocks
    Handler handler = new Handler(){};
    @BeforeEach
    public void init(){
        client = mock(Client.class);
        MockitoAnnotations.initMocks(this);

    }
    @Test
    public void testAllSuccess() {
        String idOrder = "1";
        String status = "OK";
        when(client.getApplicationStatus1(idOrder)) //todo не работает с интерфейсом
                .thenReturn(new Response.Success(status, "123"));
        when(client.getApplicationStatus2(idOrder))
                .thenReturn(new Response.Success(status, "123"));
        ApplicationStatusResponse response = handler.performOperation(idOrder);
        ApplicationStatusResponse expected = new ApplicationStatusResponse.Success(idOrder, status);
        assertEquals(response, expected);
    }

}
