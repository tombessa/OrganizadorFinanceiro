package br.com.organizadorfinanceiro.imports;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.import-storage")
public class ImportStorageProperties {
    private String root = "/var/lib/organizador-financeiro/imports";
    private String supabaseUrl;
    private String bucket = "financial-imports";

    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public String getSupabaseUrl() { return supabaseUrl; }
    public void setSupabaseUrl(String supabaseUrl) { this.supabaseUrl = supabaseUrl; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
}
