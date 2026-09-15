param(
    [ValidateSet("admin", "manager", "warehouse", "user")]
    [string]$Role = "admin",

    [string]$KeycloakUrl = "http://localhost:8081",
    [string]$Realm = "autosalon",
    [string]$ClientId = "autosalon-app",
    [string]$ClientSecret = "autosalon-secret"
)

$body = @{
    grant_type    = "password"
    client_id     = $ClientId
    client_secret = $ClientSecret
    username      = $Role
    password      = $Role
}

$uri = "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token"

try {
    $response = Invoke-RestMethod -Method Post -Uri $uri -Body $body
} catch {
    Write-Error "Token request failed: $($_.Exception.Message)"
    exit 1
}

$token = $response.access_token
$token | Set-Clipboard

$payload = $token.Split(".")[1].Replace("-", "+").Replace("_", "/")
switch ($payload.Length % 4) { 2 { $payload += "==" } 3 { $payload += "=" } }
$claims = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload)) | ConvertFrom-Json

"user:    $($claims.preferred_username)"
"sub:     $($claims.sub)"
"roles:   $($claims.realm_access.roles -join ', ')"
"expires: $([DateTimeOffset]::FromUnixTimeSeconds($claims.exp).LocalDateTime)"
"Bearer token copied to clipboard."
