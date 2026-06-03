inputs=${1:-0}

series="synthetic"

models=()

for s in ${series[@]}; do
    experiments=($(ls ../results/$s | sed "s/\(.*\)-.*/\1/"))
    for e in ${experiments[@]}; do
        if [[ ! $(echo ${models[@]} | fgrep -w $e) ]]; then
            models[${#models[@]}]=$e
        fi
    done
done

for m in ${models[@]}; do
    ./collate_experiment.sh $series $m
    printf "\n\n"
done


