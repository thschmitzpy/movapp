package com.loja.movapp.exception;

import java.time.LocalDateTime;

public class ErroResponse {

    private int status;
    private String mensagem;
    private String caminho;
    private LocalDateTime timestamp;

    public ErroResponse(int status, String mensagem, String caminho) {
        this.status    = status;
        this.mensagem  = mensagem;
        this.caminho   = caminho;
        this.timestamp = LocalDateTime.now();
    }

    public int           getStatus()    { return status;    }
    public String        getMensagem()  { return mensagem;  }
    public String        getCaminho()   { return caminho;   }
    public LocalDateTime getTimestamp() { return timestamp; }
}