cd C:\Users\carin\Documents\FaPra\ss22_fp_mapc_gruppe2\server\target
Write-Host "Starting Server"
Start-Process -FilePath java -ArgumentList '-jar server-2020-2.0-jar-with-dependencies.jar -conf ../confNurEinAgent/Turnier3Config.json --monitor'

Start-Sleep -s 2

cd..
cd..
cd .\javaagents\target\
Write-Host "Starting Agents"
Start-Process -FilePath java -ArgumentList '-jar javaagents-2022-1.1-jar-with-dependencies.jar'

cd..
cd..
cd .\server\target\