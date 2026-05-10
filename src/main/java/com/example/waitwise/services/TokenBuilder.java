package com.example.waitwise.services;
import com.example.waitwise.models.Citizen;
import com.example.waitwise.models.Service;
import com.example.waitwise.models.Token;

public class TokenBuilder {
    private Token token = new Token();
    public TokenBuilder setCitizen(Citizen citizen) { this.token.setCitizen(citizen); return this; }
    public TokenBuilder setService(Service service) { this.token.setService(service); return this; }
    public TokenBuilder setPriority(String priority) { this.token.setPriorityType(priority); return this; }
    public TokenBuilder generateNumber(String prefix, int count) { this.token.setTokenNumber(prefix + "-" + count); return this; }
    public Token build() { return this.token; }
}