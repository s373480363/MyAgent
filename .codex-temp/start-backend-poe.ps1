$env:OPENAI_API_KEY = 'sk-poe-nn2lNWj_er7kV1pFUFj1ckN0Io4vmhuTmucg-ekk_E0'
$env:SPRING_AI_OPENAI_BASE_URL = 'https://api.poe.com'
$env:MYAGENT_OPENAI_DEFAULT_MODEL = 'gpt-5.4'
& 'D:\myproject\MyAgent\11_code\scripts\start-backend-local.ps1' -DatasourceUrl 'jdbc:postgresql://127.0.0.1:15432/myagent' -DatasourceUsername 'myagent' -DatasourcePassword 'myagent'
