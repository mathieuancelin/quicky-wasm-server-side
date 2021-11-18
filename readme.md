# Quicky WASM server-side

## build and run

```sh
cargo install wasm-pack 
sh ./build_plugins.sh # compile the plugins
sbt run # run the jvm server
```

## test it

```sh
curl http://localhost:9001/plugins/plugin1.wasm
curl http://localhost:9001/plugins/plugin2.wasm/ -H 'Serlian: Anne Roquain'
```