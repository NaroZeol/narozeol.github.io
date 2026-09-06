package top.narozeol.thoughts;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class Account {
    private static final String ALIAS="naro-thoughts-session";
    private final SharedPreferences prefs;
    Account(Context context){prefs=context.getSharedPreferences("account",Context.MODE_PRIVATE);}
    private SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(!store.containsAlias(ALIAS)){KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());generator.generateKey();}
        return (SecretKey)store.getKey(ALIAS,null);
    }
    void save(String token) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());
        if(!prefs.edit().putString("token",Base64.encodeToString(cipher.doFinal(token.getBytes("UTF-8")),Base64.NO_WRAP)).putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).commit())throw new Exception("无法保存登录信息");
    }
    String token() throws Exception {
        String encoded=prefs.getString("token",null);if(encoded==null)return "";
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(encoded,Base64.NO_WRAP)),"UTF-8");
    }
    void clear(){prefs.edit().clear().commit();}
}
