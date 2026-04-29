package es.altia.domeadapter.backend.shared.infrastructure.crypto;

import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import es.altia.domeadapter.backend.shared.domain.util.UVarInt;
import lombok.RequiredArgsConstructor;
import org.bitcoinj.base.Base58;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;

@Configuration
@RequiredArgsConstructor
public class CryptoComponent {

    private final CryptoConfig cryptoConfig;

    @Bean
    public ECKey getECKey() {
        return buildEcKeyFromPrivateKey();
    }

    private ECKey buildEcKeyFromPrivateKey() {
        try {
            BigInteger privateKeyInt = new BigInteger(cryptoConfig.getPrivateKey(), 16);
            ECParameterSpec ecSpec   = ECNamedCurveTable.getParameterSpec("secp256r1");
            KeyFactory keyFactory    = KeyFactory.getInstance("EC", BouncyCastleProviderSingleton.getInstance());

            ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(privateKeyInt, ecSpec);
            ECPrivateKey privateKey         = (ECPrivateKey) keyFactory.generatePrivate(privateKeySpec);

            ECPublicKeySpec publicKeySpec = new ECPublicKeySpec(ecSpec.getG().multiply(privateKeyInt), ecSpec);
            ECPublicKey publicKey         = (ECPublicKey) keyFactory.generatePublic(publicKeySpec);

            return new ECKey.Builder(Curve.P_256, publicKey)
                    .privateKey(privateKey)
                    .keyID(generateDidKey(publicKey))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Error creating ECKey from private key: " + e.getMessage(), e);
        }
    }

    private String generateDidKey(ECPublicKey ecPublicKey) {
        try {
            byte[] encodedKey    = ecPublicKey.getEncoded();
            KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProviderSingleton.getInstance());

            X509EncodedKeySpec keySpec  = new X509EncodedKeySpec(encodedKey);
            BCECPublicKey bcPublicKey   = (BCECPublicKey) keyFactory.generatePublic(keySpec);
            byte[] pubKeyBytes          = bcPublicKey.getQ().getEncoded(true);

            String multiBase58Btc = convertRawKeyToMultiBase58Btc(pubKeyBytes, 0x1200);
            return "did:key:z" + multiBase58Btc;
        } catch (Exception e) {
            throw new IllegalStateException("Error converting public key to did:key: " + e.getMessage(), e);
        }
    }

    private String convertRawKeyToMultiBase58Btc(byte[] publicKey, int code) {
        UVarInt codeVarInt = new UVarInt(code);
        int totalLength    = publicKey.length + codeVarInt.getLength();
        byte[] combined    = new byte[totalLength];

        System.arraycopy(codeVarInt.getBytes(), 0, combined, 0, codeVarInt.getLength());
        System.arraycopy(publicKey, 0, combined, codeVarInt.getLength(), publicKey.length);

        return Base58.encode(combined);
    }
}
