package br.com.fiap.cuidar.service;

import br.com.fiap.cuidar.dao.ContaDAO;
import br.com.fiap.cuidar.dao.SessaoDAO;
import br.com.fiap.cuidar.dto.CadastroContaRequest;
import br.com.fiap.cuidar.dto.LoginRequest;
import br.com.fiap.cuidar.models.Conta;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class AutenticacaoService {
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final Pattern BCRYPT = Pattern.compile("\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}");
    private final ContaDAO contas;
    private final SessaoDAO sessoes;
    private final PasswordEncoder encoder;
    private final SecureRandom random = new SecureRandom();
    private final String hashFicticio;
    private final long validadeSegundos;

    public AutenticacaoService(ContaDAO contas, SessaoDAO sessoes, PasswordEncoder encoder,
                               @Value("${CUIDAR_AUTH_SESSION_TTL:PT8H}") Duration validade) {
        this.contas = contas;
        this.sessoes = sessoes;
        this.encoder = encoder;
        this.hashFicticio = encoder.encode(novoToken());
        this.validadeSegundos = validade.toSeconds();
        if (validadeSegundos < 1) throw new IllegalArgumentException("A duração da sessão deve ser positiva.");
    }

    public Conta cadastrar(CadastroContaRequest request) {
        validarTamanhoSenha(request.senha());
        return contas.cadastrar(request.nome(), normalizar(request.email()), encoder.encode(request.senha()), request.perfil());
    }

    public Sessao login(LoginRequest request) {
        validarTamanhoSenha(request.senha());
        var credencial = contas.buscarCredencial(normalizar(request.email()))
                .filter(conta -> conta.hash() != null && BCRYPT.matcher(conta.hash()).matches());
        if (!encoder.matches(request.senha(), credencial.map(ContaDAO.Credencial::hash).orElse(hashFicticio))
                || credencial.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }
        Conta conta = credencial.orElseThrow().conta();
        String token = novoToken();
        sessoes.criar(hash(token), conta.id(), validadeSegundos);
        return new Sessao(token, validadeSegundos, conta);
    }

    public Optional<Conta> consultar(String token) {
        return TOKEN.matcher(token).matches() ? sessoes.buscarAtiva(hash(token)) : Optional.empty();
    }

    public void logout(String token) {
        sessoes.revogar(hash(token));
    }

    private String novoToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalizar(String email) {
        return email.toLowerCase(Locale.ROOT);
    }

    private static void validarTamanhoSenha(String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter até 72 bytes em UTF-8.");
        }
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public record Sessao(String token, long expiraEmSegundos, Conta conta) {
        @Override
        public String toString() {
            return "Sessao[contaId=" + conta.id() + ", expiraEmSegundos=" + expiraEmSegundos + "]";
        }
    }
}
