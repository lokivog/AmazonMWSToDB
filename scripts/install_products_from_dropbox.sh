#!/bin/bash

mkdir -p build/products
rsync -rvc /amazon-dropbox/Dropbox/amazonproducts/json/ build/products/
gunzip build/products/*.gz
