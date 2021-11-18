loc=`pwd`

cd $loc/plugin1
wasm-pack build --target web
cd $loc/plugin2
wasm-pack build --target web
cd $loc
cp $loc/plugin1/target/wasm32-unknown-unknown/release/plugin1.wasm $loc/plugin1.wasm
cp $loc/plugin2/target/wasm32-unknown-unknown/release/plugin2.wasm $loc/plugin2.wasm
