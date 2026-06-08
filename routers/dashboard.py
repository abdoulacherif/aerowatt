from fastapi import APIRouter
from fastapi.responses import FileResponse

router = APIRouter(prefix="/dashboard", tags=["dashboard"])

@router.get("/")
async def dashboard():
    return FileResponse("templates/dashboard.html")
