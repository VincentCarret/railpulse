param([int]$Port = 8080)

$ErrorActionPreference = 'Stop'
$racineProjet = Split-Path -Parent $PSScriptRoot
Push-Location -LiteralPath $racineProjet
try {
    # Lire uniquement les secrets de demonstration, sans executer le contenu du fichier.
    if (Test-Path -LiteralPath '.env') {
        foreach ($ligne in Get-Content -LiteralPath '.env' -Encoding utf8) {
            if ($ligne -match '^(RAILPULSE_DEMO_(AGENT|RESPONSABLE)_PASSWORD)=(.*)$') {
                $nomVariable = $Matches[1]
                $valeurVariable = $Matches[3].Trim().Trim('"').Trim("'")
                [Environment]::SetEnvironmentVariable($nomVariable, $valeurVariable, 'Process')
            }
        }
    }
    foreach ($nomVariable in @('RAILPULSE_DEMO_AGENT_PASSWORD', 'RAILPULSE_DEMO_RESPONSABLE_PASSWORD')) {
        if (-not [Environment]::GetEnvironmentVariable($nomVariable, 'Process')) {
            $saisieSecrete = Read-Host "$nomVariable (12 caracteres minimum)" -AsSecureString
            $valeurVariable = [System.Net.NetworkCredential]::new('', $saisieSecrete).Password
            [Environment]::SetEnvironmentVariable($nomVariable, $valeurVariable, 'Process')
        }
    }
    & .\mvnw.cmd -B -ntp -DskipTests package
    if ($LASTEXITCODE -ne 0) { throw 'La construction du projet a echoue.' }
    Write-Host "Rail Pulse : http://localhost:$Port/login - comptes demo.agent et demo.responsable."
    # Les arguments explicites empechent une variable Supabase/MySQL heritee de cibler la base distante.
    & java -jar target/railpulse-0.0.1-SNAPSHOT.jar '--spring.profiles.active=demo-local,demo' `
        '--spring.datasource.url=jdbc:h2:file:./.data/railpulse;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE' `
        '--spring.datasource.driver-class-name=org.h2.Driver' '--spring.datasource.username=sa' `
        '--spring.datasource.password=' "--server.port=$Port"
} finally {
    Pop-Location
}
