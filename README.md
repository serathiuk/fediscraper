# Mastodon dados de bloqueio

Geração e informações de dados de bloqueios e suspensões de instàncias do fediverso.

## Arquivos
* [dados/instances.csv](dados/instances.csv) - Lista de instâncias ativas, gerada pelo site [https://instances.social](instances.social).
* [dados/categories.csv](dados/categories.csv) - Categorias das instâncias.
* [dados/languages.csv](dados/languages.csv) - Idiomas das instâncias.
* [dados/prohibited_content.csv](dados/prohibited_content.csv) - Informações proíbidas nas instâncias.
* [dados/blocks.csv](dados/blocks.csv) - Lista de instâncias bloqueadas ou mutadas, exportada da própria instância, pelo endpoint https://[URL_DA_INSTANCIA]/api/v1/instance/domain_blocks.

## Como rodar o script

* Criar o arquivo src/main/java/resources/config.properties com as seguintes informações:
* Configurar "instances.social.key" com a chave de acesso da API do instances.social.
* Configurar "app.files.path" com o destino dos arquivos de saída.
* Rodar o arquivo dev.serathiuk.Main.