"""Overnight coordinated expert-context reconfirmation for Flower 14/17.

Uses iterative best response across four policy families:
  Buy, Cultivation Main, Plant Effect/Targeting, Battle Support.
Battle Main and all other families remain Human Baseline.

Each replicate is independent. Cycle 1 bootstraps the three surrounding policies
around a different certified Buy learner, then trains a fresh Buy policy from
zero. Later cycles warm-start every family and retrain it against the current
other three policies. Every cycle receives held-out market evaluation.
"""
from __future__ import annotations

import argparse, csv, os, shutil, sys
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence

from leaf_experiments.paths import find_project_root
from leaf_experiments.timing import ExperimentPlanRunner, ExperimentStep
from .confirmation import create_archive, default_process_executor
from .reader import MARKET_SCHEMA_VERSION, load_market_result

@dataclass(frozen=True)
class Config:
    project_root: Path
    certified_root: Path
    output_root: Path
    player_counts: tuple[int,...]=(2,4)
    replicates: int=3
    cycles: int=3
    generations: int=8
    population: int=10
    train_games: int=60
    cycle_eval_games: int=1000
    final_eval_games: int=4000
    elites: int=2
    sigma: float=.25
    mutations: int=8
    resume: bool=True
    rounds: str="3/2/2"
    plant_overrides: Path|None=None
    round_overrides: Path|None=None
    def __post_init__(self):
        r=self.project_root.resolve(); object.__setattr__(self,"project_root",r)
        for f in ("certified_root","output_root"):
            v=getattr(self,f); object.__setattr__(self,f,v if v.is_absolute() else r/v)
        object.__setattr__(self,"plant_overrides", self.plant_overrides or r/"data/research/4p/resync/resync-current.csv")
        object.__setattr__(self,"round_overrides", self.round_overrides or r/"data/research/4p/resync/round-resync-current.csv")

def cert_buy(c,p,r): return c.certified_root/f"{p}p/weights/learner-{r}.weights"
def base(c,p,r): return c.output_root/f"{p}p/replicate-{r}"
def w(c,p,r,cy,fam): return base(c,p,r)/f"cycle-{cy}/weights/{fam}.weights"
def log(c,p,r,cy,name): return base(c,p,r)/f"cycle-{cy}/logs/{name}.log"
def raw(c,p,r,cy): return base(c,p,r)/f"cycle-{cy}/eval/expert-context.market.json"
def mark(x): return Path(str(x)+".complete")
def complete(logp,out): return c_resume and mark(logp).is_file() and out.is_file()

c_resume=True

def seed(p,r,cy,slot):
    return 12_000_000 + p*1_000_000 + r*100_000 + cy*10_000 + slot*1000

def common(c,p,r,cy,slot):
    b=seed(p,r,cy,slot)
    return ["--players",str(p),"--generations",str(c.generations),"--population",str(c.population),"--games",str(c.train_games),"--elites",str(c.elites),"--sigma",str(c.sigma),"--mutations",str(c.mutations),"--evolution-seed",str(b+1),"--seed",str(b+101),"--strategy-seed",str(b+201),"--random-grove","--grove-seed",str(b+301),"--rounds",c.rounds,"--plant-overrides",str(c.plant_overrides),"--round-overrides",str(c.round_overrides)]

def prior_buy(c,p,r,cy): return cert_buy(c,p,r) if cy==1 else w(c,p,r,cy-1,"buy")
def prior(c,p,r,cy,fam): return c.output_root/"config"/f"START_ZERO_{fam}.weights" if cy==1 else w(c,p,r,cy-1,fam)

def train_cmd(c,p,r,cy,fam):
    buy=prior_buy(c,p,r,cy)
    cm = w(c,p,r,cy,"cultivation-main") if fam!="cultivation-main" and w(c,p,r,cy,"cultivation-main").exists() else (w(c,p,r,cy-1,"cultivation-main") if cy>1 else None)
    pe = w(c,p,r,cy,"plant-effect") if fam not in ("cultivation-main","plant-effect") and w(c,p,r,cy,"plant-effect").exists() else (w(c,p,r,cy-1,"plant-effect") if cy>1 else None)
    bs = w(c,p,r,cy,"battle-support") if fam=="buy" and w(c,p,r,cy,"battle-support").exists() else (w(c,p,r,cy-1,"battle-support") if cy>1 else None)
    if fam=="cultivation-main":
        cmd=[str(c.project_root/"bin/train_cultivation_main_policy"),*common(c,p,r,cy,1),"--input",str(prior(c,p,r,cy,fam)),"--output",str(w(c,p,r,cy,fam)),"--buy-policy","learned","--buy-weights",str(buy)]
        if pe: cmd += ["--plant-effect-policy","learned","--plant-effect-weights",str(pe)]
        if bs: cmd += ["--battle-support-policy","learned","--battle-support-weights",str(bs)]
        return cmd
    if fam=="plant-effect":
        cmd=[str(c.project_root/"bin/train_plant_effect_policy"),*common(c,p,r,cy,2),"--input",str(prior(c,p,r,cy,fam)),"--output",str(w(c,p,r,cy,fam)),"--buy-policy","learned","--buy-weights",str(buy),"--cultivation-main-policy","learned","--cultivation-main-weights",str(w(c,p,r,cy,"cultivation-main"))]
        if bs: cmd += ["--battle-support-policy","learned","--battle-support-weights",str(bs)]
        return cmd
    if fam=="battle-support":
        return [str(c.project_root/"bin/train_battle_support_policy"),*common(c,p,r,cy,3),"--input",str(prior(c,p,r,cy,fam)),"--output",str(w(c,p,r,cy,fam)),"--buy-policy","learned","--buy-weights",str(buy),"--cultivation-main-policy","learned","--cultivation-main-weights",str(w(c,p,r,cy,"cultivation-main")),"--plant-effect-policy","learned","--plant-effect-weights",str(w(c,p,r,cy,"plant-effect"))]
    if fam=="buy":
        return [str(c.project_root/"bin/train_buy_policy"),*common(c,p,r,cy,4),"--input",str(prior(c,p,r,cy,fam)),"--output",str(w(c,p,r,cy,fam)),"--cultivation-main-policy","learned","--cultivation-main-weights",str(w(c,p,r,cy,"cultivation-main")),"--plant-effect-policy","learned","--plant-effect-weights",str(w(c,p,r,cy,"plant-effect")),"--battle-support-policy","learned","--battle-support-weights",str(w(c,p,r,cy,"battle-support"))]
    raise ValueError(fam)

def eval_cmd(c,p,r,cy):
    games=c.final_eval_games if cy==c.cycles else c.cycle_eval_games
    b=seed(p,r,cy,9)
    return [str(c.project_root/"bin/evaluate_buy_policy"),"--players",str(p),"--weights",str(w(c,p,r,cy,"buy")),"--games",str(games),"--seed",str(b+101),"--strategy-seed",str(b+201),"--random-grove","--grove-seed",str(b+301),"--rounds",c.rounds,"--plant-overrides",str(c.plant_overrides),"--round-overrides",str(c.round_overrides),"--cultivation-main-policy","learned","--cultivation-main-weights",str(w(c,p,r,cy,"cultivation-main")),"--plant-effect-policy","learned","--plant-effect-weights",str(w(c,p,r,cy,"plant-effect")),"--battle-support-policy","learned","--battle-support-weights",str(w(c,p,r,cy,"battle-support")),"--market-json",str(raw(c,p,r,cy))]

def runproc(cmd,logp,root):
    logp.parent.mkdir(parents=True,exist_ok=True); default_process_executor(cmd,logp,root); mark(logp).touch()

def eval_complete(c,p,r,cy):
    lp=log(c,p,r,cy,"evaluate"); rp=raw(c,p,r,cy)
    if not (c.resume and mark(lp).is_file() and rp.is_file()): return False
    try: return load_market_result(rp).schema_version==MARKET_SCHEMA_VERSION
    except Exception: return False

def validate(c):
    for rel in ("bin/train_buy_policy","bin/train_cultivation_main_policy","bin/train_plant_effect_policy","bin/train_battle_support_policy","bin/evaluate_buy_policy"):
        q=c.project_root/rel
        if not q.is_file() or not os.access(q,os.X_OK): raise FileNotFoundError(q)
    for q in (c.plant_overrides,c.round_overrides):
        if not q.is_file(): raise FileNotFoundError(q)
    for p in c.player_counts:
        for r in range(1,c.replicates+1):
            if not cert_buy(c,p,r).is_file(): raise FileNotFoundError(f"missing bootstrap certified Buy learner: {cert_buy(c,p,r)}")

def write_config(c):
    d=c.output_root/"config"; d.mkdir(parents=True,exist_ok=True)
    for fam in ("buy","cultivation-main","plant-effect","battle-support"): (d/f"START_ZERO_{fam}.weights").unlink(missing_ok=True)
    shutil.copy2(c.plant_overrides,d/"plant-overrides.csv"); shutil.copy2(c.round_overrides,d/"round-overrides.csv")
    (d/"experiment.txt").write_text(f"experiment=Flower coordinated expert-context reconfirmation\nplayers={c.player_counts}\nreplicates={c.replicates}\ncycles={c.cycles}\ntraining={c.generations} generations x {c.population} population x {c.train_games} games/policy\ncycleEval={c.cycle_eval_games}\nfinalEval={c.final_eval_games}\nmethod=iterative best response; Buy+Cultivation Main+Plant Effect+Battle Support adaptive; Battle Main and other policies Human\n",encoding="utf-8")

def write_reports(c):
    d=c.output_root/"reports"; d.mkdir(parents=True,exist_ok=True)
    card_rows=[]; tier={}
    for p in c.player_counts:
      for r in range(1,c.replicates+1):
       for cy in range(1,c.cycles+1):
        x=load_market_result(raw(c,p,r,cy))
        for card in x.cards:
            if card.plant_type!="FLOWER" or card.cost not in (14,17): continue
            op=card.learned_opportunity; rate=100*op.selected_decisions/op.legal_decisions if op.legal_decisions else 0
            card_rows.append((p,r,cy,card.identity,card.cost,op.legal_decisions,op.selected_decisions,rate,x.learned.win_share))
            a=tier.setdefault((p,r,cy,card.cost),[0,0]); a[0]+=op.legal_decisions; a[1]+=op.selected_decisions
    with (d/"expert-context-card-trajectory.tsv").open("w",newline="",encoding="utf-8") as f:
        z=csv.writer(f,delimiter="\t"); z.writerow(["players","replicate","cycle","identity","cost","legal","selected","select_when_legal_pct","buy_win_share"]); z.writerows([(*a[:-2],f"{a[-2]:.4f}",f"{a[-1]:.6f}") for a in card_rows])
    with (d/"expert-context-tier-trajectory.tsv").open("w",newline="",encoding="utf-8") as f:
        z=csv.writer(f,delimiter="\t"); z.writerow(["players","replicate","cycle","cost","legal","selected","select_when_legal_pct"])
        for k,(le,se) in sorted(tier.items()): z.writerow([*k,le,se,f"{100*se/le if le else 0:.4f}"])
    with (d/"final-replicate-summary.tsv").open("w",newline="",encoding="utf-8") as f:
        z=csv.writer(f,delimiter="\t"); z.writerow(["players","replicate","cost","legal","selected","select_when_legal_pct"])
        for (p,r,cy,cost),(le,se) in sorted(tier.items()):
            if cy==c.cycles: z.writerow([p,r,cost,le,se,f"{100*se/le if le else 0:.4f}"])
    (d/"README-FIRST.txt").write_text("COORDINATED EXPERT-CONTEXT RECONFIRMATION\n\nThree independent replicates per player count by default. Four policy families adapt by iterative best response: Cultivation Main -> Plant Effect -> Battle Support -> Buy. Cycle 1 bootstraps from a different certified Buy learner but trains a fresh Buy from zero after the surrounding context is built. Later cycles warm-start all four families against the latest other policies.\n\nPrimary question: after coordinated adaptation, do 2p Flower 17 and 4p Flower 14 remain near-dead when legal?\n\nStrong confirmation: the problematic tier remains near zero across final independent replicates and does not trend upward over cycles.\nContext artifact evidence: the tier revives materially and repeatably after coordinated adaptation.\n",encoding="utf-8")

def run(c):
    global c_resume; c_resume=c.resume
    validate(c); c.output_root.mkdir(parents=True,exist_ok=True); write_config(c)
    print("FLOWER 14/17 — OVERNIGHT COORDINATED EXPERT-CONTEXT RECONFIRMATION",flush=True)
    print(f"players={c.player_counts} replicates={c.replicates} cycles={c.cycles}",flush=True)
    print("Adaptive families: Buy + Cultivation Main + Plant Effect + Battle Support",flush=True)
    print("Battle Main and remaining policy families stay Human Baseline.",flush=True)
    steps=[]
    for p in c.player_counts:
      for r in range(1,c.replicates+1):
       for cy in range(1,c.cycles+1):
        for fam in ("cultivation-main","plant-effect","battle-support","buy"):
            lp=log(c,p,r,cy,f"train-{fam}"); out=w(c,p,r,cy,fam); out.parent.mkdir(parents=True,exist_ok=True)
            skip=c.resume and mark(lp).is_file() and out.is_file()
            steps.append(ExperimentStep(label=f"Train {fam} {p}p replicate {r}/{c.replicates} cycle {cy}/{c.cycles}",category=f"train-{fam}-{p}p",skip=skip,action=lambda p=p,r=r,cy=cy,fam=fam,lp=lp: runproc(train_cmd(c,p,r,cy,fam),lp,c.project_root)))
        lp=log(c,p,r,cy,"evaluate"); raw(c,p,r,cy).parent.mkdir(parents=True,exist_ok=True)
        steps.append(ExperimentStep(label=f"Evaluate coordinated context {p}p replicate {r}/{c.replicates} cycle {cy}/{c.cycles}",category=f"eval-{p}p",skip=eval_complete(c,p,r,cy),action=lambda p=p,r=r,cy=cy,lp=lp: runproc(eval_cmd(c,p,r,cy),lp,c.project_root)))
    ExperimentPlanRunner(steps).run(description="OVERNIGHT COORDINATED EXPERT-CONTEXT RECONFIRMATION complete")
    write_reports(c)
    ar=c.output_root/"flower-expert-context-overnight-results.tar.gz"; create_archive(c.output_root,ar); print(f"Archive: {ar}",flush=True); return ar

def main(argv:Sequence[str]|None=None):
    if hasattr(sys.stdout,"reconfigure"): sys.stdout.reconfigure(line_buffering=True)
    root=find_project_root(); ap=argparse.ArgumentParser(description="Overnight coordinated expert-context Flower reconfirmation")
    ap.add_argument("--certified-root",type=Path,default=Path("output/experiments/current-market-confirmation-certified")); ap.add_argument("--output-root",type=Path,default=Path("output/experiments/flower-expert-context-overnight")); ap.add_argument("--players",nargs="+",type=int,choices=(2,3,4),default=[2,4]); ap.add_argument("--replicates",type=int,default=3); ap.add_argument("--cycles",type=int,default=3); ap.add_argument("--generations",type=int,default=8); ap.add_argument("--population",type=int,default=10); ap.add_argument("--train-games",type=int,default=60); ap.add_argument("--cycle-eval-games",type=int,default=1000); ap.add_argument("--final-eval-games",type=int,default=4000); ap.add_argument("--resume",choices=("0","1"),default="1")
    a=ap.parse_args(argv); run(Config(root,a.certified_root,a.output_root,tuple(a.players),a.replicates,a.cycles,a.generations,a.population,a.train_games,a.cycle_eval_games,a.final_eval_games,resume=a.resume=="1")); return 0
if __name__=="__main__": raise SystemExit(main())
