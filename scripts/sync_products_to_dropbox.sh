#!/bin/bash

rsync -rvc --ignore-existing --exclude=*.json output/json/ /amazon-dropbox/Dropbox/amazonproducts/json/
rsync -rvc --ignore-existing --exclude=*.xml output/xml/ /amazon-dropbox/Dropbox/amazonproducts/xml/
